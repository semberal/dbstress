package eu.semberal.dbstress.actor

import java.lang.Class.forName
import java.sql.{Connection, DriverManager}
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent._

import akka.actor.{Actor, LoggingFSM}
import com.typesafe.scalalogging.slf4j.LazyLogging
import eu.semberal.dbstress.actor.DbCommunicationActor._
import eu.semberal.dbstress.actor.UnitRunActor.{DbCallFinished, DbConnInitFinished}
import eu.semberal.dbstress.model.Configuration._
import eu.semberal.dbstress.model.Results._
import eu.semberal.dbstress.util.ModelExtensions.ArmManagedResource
import org.joda.time.DateTime.now
import resource._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

class DbCommunicationActor(dbConfig: DbCommunicationConfig)
  extends Actor
  with LazyLogging
  with LoggingFSM[State, Option[Connection]] {

  private implicit val dbDispatcher = context.system.dispatchers.lookup("akka.dispatchers.db-dispatcher")

  private def mkTimeout(x: Option[Int]) = x.getOrElse(Int.MaxValue)

  startWith(Uninitialized, None)

  when(Uninitialized) {
    case Event(InitDbConnection, _) =>
      val start = now()

      /*
       * When a future succeeds. actual run of the future is reported as a duration.
       * When a future fails, future creation start time is reported (i.e. might include waiting for ec)
       */
      val f = Future {
        val fStart = now()
        dbConfig.driverClass.foreach(forName)
        val connection = DriverManager.getConnection(dbConfig.uri, dbConfig.username, dbConfig.password)
        val fEnd = now()
        (fStart, fEnd, connection)
      }

      try {
        /* not ideal, but connection reference is needed to make the state transition */
        val (s, e, connection) = Await.result(f, Duration.create(mkTimeout(dbConfig.connectionTimeout), MILLISECONDS))
        logger.debug("Database connection has been successfully created")
        goto(WaitForJob) using Some(connection) replying DbConnInitFinished(DbConnInitSuccess(s, e))
      } catch {
        case e: Throwable =>
          logger.debug("An error during connection initialization has occurred", e)
          stay() replying DbConnInitFinished(DbConnInitFailure(start, now(), e))
      }
  }

  when(WaitForJob) {
    case Event(NextRound, connection) =>
      val start = now()

      val dbFuture = Future {
        val futStart = now()
        val statementResult = connection.map { conn =>
          managed(conn.createStatement()).map({ statement =>
            if (statement.execute(dbConfig.query)) {
              val resultSet = statement.getResultSet
              val fetchedRows = Iterator.continually(resultSet.next()).takeWhile(identity).length
              FetchedRows(fetchedRows)
            } else {
              UpdateCount(statement.getUpdateCount)
            }
          }).toTry match {
            case Success(x) => x
            case Failure(e) => throw e
          }
        }.getOrElse(throw new IllegalStateException("Connection has not been initialized, yet. "))
        val futEnd = now()
        (futStart, futEnd, statementResult)
      }

      val timeoutFuture = akka.pattern.after(
        Duration.create(mkTimeout(dbConfig.queryTimeout), MILLISECONDS),
        using = context.system.scheduler
      ) {
        throw new TimeoutException("Database call has timed out")
      }

      val currentSender = sender() // do not close over sender() in a Future!!!
      Future.firstCompletedOf(dbFuture :: timeoutFuture :: Nil).onComplete {
        case Success((s, e, fetched)) =>
          currentSender ! DbCallFinished(DbCallSuccess(s, e, fetched))
        case Failure(e) =>
          currentSender ! DbCallFinished(DbCallFailure(start, now(), e))
      }
      stay()
  }

  onTermination {
    case StopEvent(_, _, connection) =>
      logger.debug("Closing database connection")
      try {
        connection.foreach(_.close())
        logger.debug("Database connection has been successfully closed")
      } catch {
        case e: Throwable =>
          logger.warn("Unable to close a database connection", e)
      }
  }

  initialize()
}

object DbCommunicationActor {

  case object InitDbConnection

  case object NextRound

  protected sealed trait State

  protected case object Uninitialized extends State

  protected case object WaitForJob extends State

}
