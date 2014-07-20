package eu.semberal.dbstress.actor

import java.sql.{Connection, DriverManager}
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeoutException

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
import scala.concurrent.{Await, Future, blocking}
import scala.util.{Failure, Success}

class DbCommunicationActor(dbConfig: DbCommunicationConfig) extends Actor with LazyLogging with LoggingFSM[State, Option[Connection]] {

  startWith(Uninitialized, None)

  when(Uninitialized) {
    case Event(InitDbConnection, _) =>
      implicit val executionContext = context.dispatcher
      val start = now()

      val f = Future {
        Class.forName(dbConfig.driverClass)
        DriverManager.getConnection(dbConfig.uri, dbConfig.username, dbConfig.password)
      }

      try {
        /* not ideal, but connection reference is needed to make the state transition */
        val connection = Await.result(f, Duration.create(dbConfig.connectionTimeout, MILLISECONDS))
        logger.debug("Database connection has been successfully created")
        goto(WaitForJob) using Some(connection) replying DbConnInitFinished(DbConnInitSuccess(start, now()))
      } catch {
        case e: Throwable =>
          logger.debug("An error during connection initialization has occurred", e)
          stay() replying DbConnInitFinished(DbConnInitFailure(start, now(), e))
      }
  }

  when(WaitForJob) {
    case Event(NextRound, connection) =>
      implicit val executionContext = context.system.dispatcher
      val start = now()

      val dbFuture: Future[StatementResult] = Future {
        connection.map { conn =>
          blocking {
            managed(conn.createStatement()).map({ statement =>
              if (statement.execute(dbConfig.query)) {
                val resultSet = statement.getResultSet // todo support multiple result sets (statement#getMoreResults)
                val fetchedRows = Iterator.continually(resultSet.next()).takeWhile(identity).length
                FetchedRows(fetchedRows)
              } else {
                UpdateCount(statement.getUpdateCount)
              }
            }).toTry match {
              case Success(x) => x
              case Failure(e) => throw e
            }
          }
        }.getOrElse(throw new IllegalStateException("Connection has not been initialized"))
      }

      val timeoutFuture = akka.pattern.after(Duration.create(dbConfig.queryTimeout, MILLISECONDS), using = context.system.scheduler) {
        throw new TimeoutException("Database call has timed out")
      }

      val s = sender() // do not close over sender() in a Future!
      Future.firstCompletedOf(Seq(dbFuture, timeoutFuture)).onComplete {
        case Success(fetched) =>
          s ! DbCallFinished(DbCallSuccess(start, now(), fetched))
        case Failure(e) =>
          s ! DbCallFinished(DbCallFailure(start, now(), e))
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
