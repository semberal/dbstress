package eu.semberal.dbstress.integration.actor

import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.{TimeUnit, TimeoutException}

import akka.actor.Props
import akka.testkit.ImplicitSender
import eu.semberal.dbstress.actor.DbCommunicationActor
import eu.semberal.dbstress.actor.DbCommunicationActor.{InitDbConnection, NextRound}
import eu.semberal.dbstress.actor.UnitRunActor.{DbCallFinished, DbConnInitFinished}
import eu.semberal.dbstress.integration.AbstractDbstressIntegrationTest
import eu.semberal.dbstress.model.Configuration.DbCommunicationConfig
import eu.semberal.dbstress.model.Results._
import org.scalatest.FlatSpecLike

import scala.concurrent.duration.Duration

class DbCommunicationActorIntegrationTest extends FlatSpecLike with ImplicitSender with AbstractDbstressIntegrationTest {

  private trait actorScope {
    protected def dbCommunicationConfig: DbCommunicationConfig

    protected lazy val actor = system.actorOf(Props(classOf[DbCommunicationActor], dbCommunicationConfig, "X", "Y"))

    protected val msgWaitTime = Duration(3, SECONDS)
  }

  "DbCommunicationActor" should "report failed connection init when db config if wrong" in new actorScope {
    val dbCommunicationConfig = DbCommunicationConfig("A", Some("B"), "C", "D", "E", None, None)
    actor ! InitDbConnection
    expectMsgPF(msgWaitTime) { case DbConnInitFinished(DbConnInitFailure(_, _, _)) => }
  }

  it should "successfully initialize a connection when db config is correct" in new actorScope {
    def dbCommunicationConfig =
      DbCommunicationConfig("jdbc:h2:mem://localhost", Some("org.h2.Driver"), "sa", "", "select 1", None, None)

    actor ! InitDbConnection
    expectMsgPF(msgWaitTime) { case DbConnInitFinished(DbConnInitSuccess(_, _)) => }
  }

  it should "correctly report db call result" in new actorScope {
    def dbCommunicationConfig: DbCommunicationConfig =
      DbCommunicationConfig("jdbc:h2:mem://localhost", Some("org.h2.Driver"), "sa", "", "select 1", None, None)

    actor ! InitDbConnection
    receiveOne(msgWaitTime)
    actor ! NextRound
    expectMsgPF(msgWaitTime) { case DbCallFinished(DbCallSuccess(_, _, _, FetchedRows(1))) => }
  }

  it should "correctly report db call error when the query timeouts" in new actorScope {
    def dbCommunicationConfig: DbCommunicationConfig = DbCommunicationConfig(
      "jdbc:h2:mem://localhost", Some("org.h2.Driver"), "sa", "",
      "CREATE ALIAS SLEEP1 FOR \"java.lang.Thread.sleep(long)\"; CALL SLEEP1(2000);", None, Some(1000))

    actor ! InitDbConnection
    receiveOne(msgWaitTime)
    actor ! NextRound
    expectMsgPF(msgWaitTime) { case DbCallFinished(DbCallFailure(_, _, _, e)) if e.isInstanceOf[TimeoutException] => }
  }

  it should "report db call success when query takes some time, but doesn't timeout" in new actorScope {

    val query: String = "CREATE ALIAS SLEEP2 FOR \"java.lang.Thread.sleep(long)\"; CALL SLEEP2(500);"
    val dbCommunicationConfig = DbCommunicationConfig("jdbc:h2:mem://localhost", Some("org.h2.Driver"), "sa", "", query, None, Some(1000))

    actor ! InitDbConnection
    receiveOne(msgWaitTime)
    actor ! NextRound
    expectMsgPF(msgWaitTime) { case DbCallFinished(DbCallSuccess(_, _, _, _)) => }
  }
}
