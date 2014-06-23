package eu.semberal.dbstress.actor

import java.lang.System.err
import java.sql.{Connection, DriverManager}

import akka.actor.Actor
import eu.semberal.dbstress.actor.DbCommActor.{Init, NextRound}
import eu.semberal.dbstress.model.{DbConfig, DbFailure, DbSuccess}
import org.duh.resource._

import scala.concurrent.Future
import scala.util.{Failure, Success}

class DbCommActor(config: DbConfig) extends Actor {

  private var connection: Option[Connection] = None

  override def postStop(): Unit = {
    connection.foreach(c => if (!c.isClosed) c.close())
    connection = None
    super.postStop()
  }

  override def receive = {
    case Init =>
      Class.forName(config.driverClass)
      connection = Some(DriverManager.getConnection(config.uri, config.username, config.password))
    case NextRound =>
      val start = System.currentTimeMillis()
      implicit val executionContext = context.system.dispatcher
      Class.forName(config.driverClass)
      val f: Future[Int] = Future {
        connection.map { c =>
          for (statement <- c.createStatement().auto) yield {
            statement.execute(config.query)
            val resultSet = statement.getResultSet
            Iterator.continually({
              val n = resultSet.next()
              if (n) 1 else 0
            }).takeWhile(_ == 1).sum
          }
        }.getOrElse(throw new IllegalStateException("Connection has not been initialized"))
      }

      f.onComplete {
        case Success(fetched) =>
          context.parent ! DbSuccess(start, System.currentTimeMillis(), fetched)
        case Failure(e) =>
          err.println(e.getMessage)
          context.parent ! DbFailure(start, System.currentTimeMillis(), e)
      }
  }
}

object DbCommActor {

  case object NextRound

  case object Init

}
