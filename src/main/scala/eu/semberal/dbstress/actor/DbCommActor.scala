package eu.semberal.dbstress.actor

import java.sql.DriverManager

import akka.actor.Actor
import eu.semberal.dbstress.model.{DbConfig, DbFailure, DbSuccess}
import org.duh.resource._

import scala.concurrent.Future
import scala.util.{Failure, Success}

class DbCommActor extends Actor {

  override def receive = {
    case DbConfig(uri, username, password, query) =>
      val start = System.currentTimeMillis()
      implicit val executionContext = context.system.dispatcher
      val f: Future[Int] = Future {
        for (connection <- DriverManager.getConnection(uri).auto;
             statement <- connection.createStatement().auto) yield {
          statement.execute(query)
          val resultSet = statement.getResultSet
          Iterator.continually({
            val n = resultSet.next()
            if (n) 1 else 0
          }).takeWhile(_ == 1).sum
        }
      }

      f.onComplete {
        case Success(fetched) =>
          context.parent ! DbSuccess(start, System.currentTimeMillis(), fetched)
        case Failure(e) =>
          context.parent ! DbFailure(start, System.currentTimeMillis(), e)
      }


  }
}
