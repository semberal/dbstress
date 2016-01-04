package eu.semberal.dbstress.actor

import java.lang.Class.forName
import java.sql.{Connection, DriverManager}
import java.util.concurrent._

import akka.actor.{Actor, PoisonPill}
import com.typesafe.scalalogging.slf4j.LazyLogging
import eu.semberal.dbstress.actor.DbCommunicationActor._
import eu.semberal.dbstress.actor.UnitRunActor.{DbCallFinished, DbConnInitFinished}
import eu.semberal.dbstress.model.Configuration._
import eu.semberal.dbstress.model.Results._
import eu.semberal.dbstress.util.IdGen._
import eu.semberal.dbstress.util.ModelExtensions.ArmManagedResource
import org.joda.time.DateTime.now
import resource._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

class DbCommunicationActor(dbConfig: DbCommunicationConfig, scenarioId: String, connectionId: String) extends Actor with LazyLogging {

  private implicit val dbDispatcher = context.system.dispatchers.lookup("akka.dispatchers.db-dispatcher")

  private[actor] var connection: Option[Connection] = None

  override def receive = {
    case InitDbConnection =>
      val start = now()

      /*
       * When a future succeeds. actual run of the future is reported as a duration.
       * When a future fails, future creation start time is reported (i.e. might include waiting for ec)
       */
      val f = Future {
        val fStart = now()
        dbConfig.driverClass.foreach(forName)
        val connection = DriverManager.getConnection(dbConfig.uri, dbConfig.username, dbConfig.password)
        (fStart, now(), connection)
      }

      try {
        /* not ideal, but connection reference is needed to make the state transition */
        val (s, e, connection) = Await.result(f, Duration(dbConfig.connectionTimeout.getOrElse(Int.MaxValue), TimeUnit.MILLISECONDS))
        logger.debug("Database connection has been successfully created")
        sender() ! DbConnInitFinished(DbConnInitSuccess(s, e))
        this.connection = Some(connection)
        context.become(waitForJob)
      } catch {
        case e: Throwable =>
          logger.debug("An error during connection initialization has occurred", e)
          sender() ! DbConnInitFinished(DbConnInitFailure(start, now(), e))
          self ! PoisonPill
      }
  }

  private[actor] def waitForJob: Receive = {

    case NextRound =>
      val start = now()

      val dbCallId = DbCallId(scenarioId, connectionId, genStatementId())
      val query = dbConfig.query.replace(IdPlaceholder, dbCallId.toString)

      val futuresList = {
        val dbFuture = Future {
          val futStart = now()
          val conn = connection.getOrElse(throw new IllegalStateException("Database connection has not been created"))

          val statementResult = managed(conn.createStatement()).map({ statement =>
            if (statement.execute(query)) {
              val resultSet = statement.getResultSet
              val fetchedRows = Iterator.continually(resultSet.next()).takeWhile(identity).length
              FetchedRows(fetchedRows)
            } else {
              UpdateCount(statement.getUpdateCount)
            }
          }).toTry.get

          (futStart, now(), statementResult)
        }

        val timeoutFuture = dbConfig.queryTimeout.map { x =>
          akka.pattern.after(Duration(x, TimeUnit.MILLISECONDS), using = context.system.scheduler) {
            throw new TimeoutException("Database call has timed out")
          }
        }

        timeoutFuture.foldRight(dbFuture :: Nil)(_ :: _)
      }

      val currentSender = sender()

      Future.firstCompletedOf(futuresList).onComplete {
        case Success((s, e, fetched)) =>
          currentSender ! DbCallFinished(DbCallSuccess(s, e, dbCallId, fetched))
        case Failure(e) =>
          currentSender ! DbCallFinished(DbCallFailure(start, now(), dbCallId, e))
      }
  }

  override def postStop(): Unit = {
    logger.debug("Closing database connection")
    try {
      connection.foreach(_.close())
      logger.debug("Database connection has been successfully closed")
    } catch {
      case e: Throwable => logger.warn("Unable to close a database connection", e)
    }
  }
}

object DbCommunicationActor {

  case object InitDbConnection

  case object NextRound

  protected sealed trait State

  protected case object Uninitialized extends State

  protected[dbstress] case object WaitForJob extends State

}
