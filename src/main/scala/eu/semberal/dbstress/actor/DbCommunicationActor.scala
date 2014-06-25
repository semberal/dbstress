package eu.semberal.dbstress.actor

import java.sql.{Connection, DriverManager}
import java.util.concurrent.TimeoutException

import akka.actor.Actor
import akka.event.LoggingReceive
import com.typesafe.scalalogging.slf4j.LazyLogging
import eu.semberal.dbstress.actor.DbCommunicationActor.{CloseConnection, InitDbConnection, NextRound}
import eu.semberal.dbstress.actor.UnitRunActor.{DbCallFinished, DbConnectionInitialized}
import eu.semberal.dbstress.model.{DbCommunicationConfig, DbFailure, DbSuccess}
import org.duh.resource._

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong
import scala.util.{Failure, Success}

class DbCommunicationActor(dbConfig: DbCommunicationConfig) extends Actor with LazyLogging {

  override def receive = LoggingReceive {
    case InitDbConnection =>
      logger.trace("Creating database connection")
      Class.forName(dbConfig.driverClass)
      val connection = DriverManager.getConnection(dbConfig.uri, dbConfig.username, dbConfig.password)
      logger.trace("Database connection has been successfully created") // todo handle errors here
      sender() ! DbConnectionInitialized
      context.become(waitForJob(connection))
  }

  def waitForJob(connection: Connection): Receive = LoggingReceive {
    case NextRound =>
      implicit val executionContext = context.system.dispatcher
      val start = System.currentTimeMillis()

      val dbFuture: Future[Int] = Future {
        for (statement <- connection.createStatement().auto) yield {
          statement.execute(dbConfig.query)
          val resultSet = statement.getResultSet
          Iterator.continually({
            val n = resultSet.next()
            if (n) 1 else 0
          }).takeWhile(_ == 1).sum
        }
      }

      val timeoutFuture = akka.pattern.after(2.millis, using = context.system.scheduler) {
        // todo timeout from configuration
        throw new TimeoutException("Database call has timed out")
      }

      Future.firstCompletedOf(Seq(dbFuture, timeoutFuture)).onComplete {
        case Success(fetched) =>
          context.parent ! DbCallFinished(DbSuccess(start, System.currentTimeMillis(), fetched))
        case Failure(e) =>
          context.parent ! DbCallFinished(DbFailure(start, System.currentTimeMillis(), e))
      }

    case CloseConnection =>
      logger.trace("Closing database connection")
      try {
        connection.close()
        logger.trace("Database connection has been successfully closed")
      } catch {
        case e: Throwable =>
          logger.warn("Unable to close a database connection", e)
      }
  }
}

object DbCommunicationActor {

  case object InitDbConnection

  case object NextRound

  case object CloseConnection

}
