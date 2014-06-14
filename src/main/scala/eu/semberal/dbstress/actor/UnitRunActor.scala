package eu.semberal.dbstress.actor

import java.sql.{DriverManager, ResultSet}

import akka.actor.{ActorLogging, Actor}
import akka.event.LoggingReceive
import eu.semberal.dbstress.model.{TestUnitConfig, UnitRunResult}
import org.duh.resource._

class UnitRunActor extends Actor with ActorLogging {

  override def receive: Receive = LoggingReceive {

    case TestUnitConfig(uri, username, password, query) =>

      for (connection <- DriverManager.getConnection(uri).auto;
           statement <- connection.createStatement().auto) {

        val start = System.currentTimeMillis()
        statement.execute(query)
        val resultSet: ResultSet = statement.getResultSet
        val fetched: Int = Iterator.continually({
          val n = resultSet.next()
          if (n) 1 else 0
        }).takeWhile(_ == 1).sum
        val end = System.currentTimeMillis()
        sender ! UnitRunResult(success = true, start, end, fetched)
      }
  }
}
