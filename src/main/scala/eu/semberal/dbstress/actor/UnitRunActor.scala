package eu.semberal.dbstress.actor

import java.sql.{ResultSet, Connection, DriverManager}

import akka.actor.Actor
import eu.semberal.dbstress.model.{UnitRunResult, TestUnitConfig}

class UnitRunActor extends Actor {

  override def receive: Receive = {

    case TestUnitConfig(uri, username, password, query) =>
      try {
      val connection: Connection = DriverManager.getConnection(uri)
      val statement = connection.createStatement()
      val start = System.currentTimeMillis()
      statement.execute(query)
      val resultSet: ResultSet = statement.getResultSet
      val fetched: Int = Iterator.continually({
        val n = resultSet.next()
        if (n) 1 else 0
      }).takeWhile(_ == 1).sum
      val end = System.currentTimeMillis()
      sender ! UnitRunResult(true, start, end, fetched)
  }
}
