package eu.semberal.dbstress.actor

import java.sql.{Connection, DriverManager}
import java.util.concurrent.TimeoutException

import akka.actor.{Actor, FSM}
import com.typesafe.scalalogging.slf4j.LazyLogging
import eu.semberal.dbstress.actor.DbCommunicationActor._
import eu.semberal.dbstress.actor.UnitRunActor.{DbCallFinished, DbConnectionInitialized}
import eu.semberal.dbstress.model._
import org.duh.resource._

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong
import scala.util.{Failure, Success}

class DbCommunicationActor(dbConfig: DbCommunicationConfig) extends Actor with LazyLogging with FSM[State, Option[Connection]] {

  startWith(Uninitialized, None)

  when(Uninitialized) {
    case Event(InitDbConnection, _) =>
      logger.trace("Creating database connection")
      Class.forName(dbConfig.driverClass)
      val connection = DriverManager.getConnection(dbConfig.uri, dbConfig.username, dbConfig.password)
      logger.trace("Database connection has been successfully created") // todo handle errors here
      context.parent ! DbConnectionInitialized
      goto(WaitForJob) using Some(connection)
  }

  when(WaitForJob) {
    case Event(NextRound, connection) =>
      implicit val executionContext = context.system.dispatcher
      val start = System.currentTimeMillis()

      val dbFuture: Future[StatementResult] = Future {
        connection.map { conn =>
          for (statement <- conn.createStatement().auto) yield {
            if (statement.execute(dbConfig.query)) {
              val resultSet = statement.getResultSet // todo support multiple result sets (statement#getMoreResults)
              val fetchedRows = Iterator.continually(resultSet.next()).takeWhile(identity).length
              FetchedRows(fetchedRows) // todo unit tests
            } else UpdateCount(statement.getUpdateCount)
          }
        }.getOrElse(throw new IllegalStateException("Connection has not been initialized"))
      }

      val timeoutFuture = akka.pattern.after(dbConfig.queryTimeout.millis, using = context.system.scheduler) {
        throw new TimeoutException("Database call has timed out")
      }

      Future.firstCompletedOf(Seq(dbFuture, timeoutFuture)).onComplete {
        case Success(fetched) =>
          context.parent ! DbCallFinished(DbSuccess(start, System.currentTimeMillis(), fetched))
        case Failure(e) =>
          context.parent ! DbCallFinished(DbFailure(start, System.currentTimeMillis(), e))
      }
      stay()
  }

  onTermination {
    case x@StopEvent(_, _, connection) => // todo unit test for closing the connection
      logger.trace("Closing database connection")
      try {
        connection.foreach(_.close())
        logger.trace("Database connection has been successfully closed")
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
