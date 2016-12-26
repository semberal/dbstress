package eu.semberal.dbstress.actor

import java.sql.{Connection, DriverManager, SQLException}

import akka.actor.{Actor, Props}
import akka.event.LoggingReceive
import com.typesafe.scalalogging.slf4j.LazyLogging
import eu.semberal.dbstress.actor.ControllerActor.{UnitRunError, UnitRunFinished, UnitRunInitializationFailed, UnitRunInitializationFinished}
import eu.semberal.dbstress.actor.DatabaseActor.{ConnectionTimeoutException, InitConnection, StartUnitRun}
import eu.semberal.dbstress.model.Configuration.UnitRunConfig
import eu.semberal.dbstress.model.Results._
import eu.semberal.dbstress.util.IdGen
import org.joda.time.DateTime
import resource._

import scala.concurrent.duration.DurationInt
import scala.concurrent.Future
import scala.util.{Failure, Success}

class DatabaseActor(scenarioId: String, unitName: String, urConfig: UnitRunConfig) extends Actor with LazyLogging {

  private var connection: Option[Connection] = None
  private var connInitResult: Option[DbConnInitResult] = None
  private val connectionId = IdGen.genConnectionId()

  private val dbDispatcher = context.system.dispatchers.lookup("akka.dispatchers.db-dispatcher")
  private val systemDispatcher = context.system.dispatcher

  override def postStop(): Unit = connection.foreach { c =>
    try c.close() catch {
      case e: SQLException => logger.warn("Cannot close database connection", e)
    }
  }

  override def receive: Receive = LoggingReceive {
    case InitConnection =>
      val s = sender()
      val start = DateTime.now()

      val future = {
        val initFuture = Future {
          urConfig.dbConfig.driverClass.foreach(Class.forName)
          DriverManager.getConnection(urConfig.dbConfig.uri, urConfig.dbConfig.username,
            urConfig.dbConfig.password)
        }(dbDispatcher)

        urConfig.dbConfig.connectionTimeout.map({ timeout =>
          val timeoutFuture = akka.pattern.after(timeout.milliseconds, context.system.scheduler) {
            Future.failed(new ConnectionTimeoutException(timeout))
          }(systemDispatcher)
          Future.firstCompletedOf(List(initFuture, timeoutFuture))(systemDispatcher)
        }).getOrElse(initFuture)
      }

      future.onComplete {
        case Success(c) =>
          s ! UnitRunInitializationFinished
          this.connection = Some(c)
          this.connInitResult = Some(DbConnInitResult(start, DateTime.now()))
        case Failure(e) =>
          s ! UnitRunInitializationFailed(new ConnectionInitException(e))
      }(systemDispatcher)

    case StartUnitRun =>

      val s = sender()

      (1 to urConfig.repeats).foldRight[Future[List[DbCallResult]]](Future.successful(Nil)) { (_, prevFuture) =>
        prevFuture.flatMap { l =>
          Future {
            val dbCallId = DbCallId(scenarioId, connectionId, IdGen.genStatementId())
            val start = DateTime.now()
            connection.map(c =>
              managed(c.createStatement()).map(statement =>
                if (statement.execute(urConfig.dbConfig.query))
                  FetchedRows(Iterator.continually(statement.getResultSet.next()).takeWhile(identity).length)
                else
                  UpdateCount(statement.getUpdateCount)
              ).tried match {
                case Success(result) =>
                  DbCallSuccess(start, DateTime.now(), dbCallId, result) :: l
                case Failure(e) =>
                  DbCallFailure(start, DateTime.now(), dbCallId, e) :: l
              }
            ).getOrElse {
              val e = new IllegalStateException("Connection not initialized")
              DbCallFailure(DateTime.now(), DateTime.now(), dbCallId, e) :: l
            }
          }(dbDispatcher)
        }(systemDispatcher)
      }.onComplete {
        case Success(l) => connInitResult match {
          case Some(irr) => s ! UnitRunFinished(unitName, UnitRunResult(irr, l))
          case None =>
            val inner = new IllegalStateException("Initialization result not available")
            val o = new UnitRunException(inner)
            s ! UnitRunError(o)
        }

        case Failure(e) => s ! UnitRunError(new UnitRunException(e))
      }(systemDispatcher)
  }
}


object DatabaseActor {
  private[actor] def props(scenarioId: String, unitName: String, unitRunConfig: UnitRunConfig): Props =
    Props(classOf[DatabaseActor], scenarioId, unitName, unitRunConfig)

  private class ConnectionTimeoutException(timeout: Int)
    extends RuntimeException(s"Database connection initialization timeout ($timeout ms)")

  private[actor] case object InitConnection

  private[actor] case object StartUnitRun

}
