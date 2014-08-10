package eu.semberal.dbstress.actor

import java.sql.Connection
import java.util.concurrent.TimeoutException

import akka.actor.{PoisonPill, Props}
import akka.testkit.TestFSMRef
import eu.semberal.dbstress.AbstractActorSystemTest
import eu.semberal.dbstress.actor.DbCommunicationActor.{InitDbConnection, NextRound}
import eu.semberal.dbstress.actor.UnitRunActor.{DbCallFinished, DbConnInitFinished}
import eu.semberal.dbstress.model.Configuration.DbCommunicationConfig
import eu.semberal.dbstress.model.Results._
import org.scalamock.scalatest.MockFactory

import scala.concurrent.duration.DurationDouble

class DbCommunicationActorTest extends AbstractActorSystemTest with MockFactory {

  trait actorScope {
    protected def dbCommunicationConfig: DbCommunicationConfig

    protected val actor = system.actorOf(Props(classOf[DbCommunicationActor], dbCommunicationConfig))
  }

  "DbCommunicationActor" should "report failed connection init when db config if wrong" in new actorScope {
      val dbCommunicationConfig = DbCommunicationConfig("A", Some("B"), "C", "D", "E", None, None)
      actor ! InitDbConnection
      expectMsgPF(5.seconds) { case DbConnInitFinished(DbConnInitFailure(_, _, _)) =>}
    }

  it should "successfully initialize a connection when db config is correct" in
    new actorScope {
      def dbCommunicationConfig =
        DbCommunicationConfig("jdbc:h2:mem://localhost", Some("org.h2.Driver"), "sa", "", "select 1", None, None)
      actor ! InitDbConnection
      expectMsgPF(5.seconds) { case DbConnInitFinished(DbConnInitSuccess(_, _)) =>}
    }

  it should "correctly report db call result" in new actorScope {
    def dbCommunicationConfig: DbCommunicationConfig =
      DbCommunicationConfig("jdbc:h2:mem://localhost", Some("org.h2.Driver"), "sa", "", "select 1", None, None)
    actor ! InitDbConnection
    receiveOne(5.seconds)
    actor ! NextRound
    expectMsgPF(5.seconds) { case DbCallFinished(DbCallSuccess(_, _, FetchedRows(1))) =>}
  }

  it should "correctly report db call error when the query timeouts" in new actorScope {
    def dbCommunicationConfig: DbCommunicationConfig = DbCommunicationConfig(
      "jdbc:h2:mem://localhost", Some("org.h2.Driver"), "sa", "",
      "CREATE ALIAS SLEEP1 FOR \"java.lang.Thread.sleep(long)\"; CALL SLEEP1(2000);", None, Some(1000))
    actor ! InitDbConnection
    receiveOne(5.seconds)
    actor ! NextRound
    expectMsgPF(2.seconds) { case DbCallFinished(DbCallFailure(_, _, e)) if e.isInstanceOf[TimeoutException] =>}
  }

  it should "report db call success when query takes some time, but doesn't timeout" in new actorScope {
    def dbCommunicationConfig: DbCommunicationConfig = DbCommunicationConfig(
      "jdbc:h2:mem://localhost", Some("org.h2.Driver"), "sa", "",
      "CREATE ALIAS SLEEP2 FOR \"java.lang.Thread.sleep(long)\"; CALL SLEEP2(500);", None, Some(1000))
    actor ! InitDbConnection
    receiveOne(5.seconds)
    actor ! NextRound
    expectMsgPF(2.seconds) { case DbCallFinished(DbCallSuccess(_, _, _)) =>}
  }

  it should "correctly close its initialized connection" in {
    val dbCommunicationConfig = DbCommunicationConfig("A", None, "C", "D", "E", None, None)
    val connection = stub[Connection]
    val actor = TestFSMRef(new DbCommunicationActor(dbCommunicationConfig))
    actor.setState(stateData = Some(connection))

    actor ! PoisonPill

    (connection.close _).verify().once()
  }
}
