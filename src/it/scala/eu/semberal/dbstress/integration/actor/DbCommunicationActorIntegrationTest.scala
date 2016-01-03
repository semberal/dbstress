package eu.semberal.dbstress.integration.actor

import java.sql.{Connection, Statement}
import java.util.concurrent.{TimeUnit, TimeoutException}
import java.util.regex.{Matcher, Pattern}

import akka.actor.{ActorRef, PoisonPill, Props}
import akka.testkit.{ImplicitSender, TestFSMRef}
import eu.semberal.dbstress.actor.DbCommunicationActor
import eu.semberal.dbstress.actor.DbCommunicationActor.{InitDbConnection, NextRound, WaitForJob}
import eu.semberal.dbstress.actor.UnitRunActor.{DbCallFinished, DbConnInitFinished}
import eu.semberal.dbstress.integration.AbstractDbstressIntegrationTest
import eu.semberal.dbstress.model.Configuration.DbCommunicationConfig
import eu.semberal.dbstress.model.Results._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpecLike, Matchers}

import scala.concurrent.duration.{FiniteDuration, DurationDouble}

class DbCommunicationActorIntegrationTest extends FlatSpecLike with Matchers with ImplicitSender with MockFactory with AbstractDbstressIntegrationTest {

  private trait actorScope {
    protected def dbCommunicationConfig: DbCommunicationConfig

    protected def actor: ActorRef

    protected val msgWaitTime = FiniteDuration(3, TimeUnit.SECONDS)
  }

  private trait realActorScope extends actorScope {
    protected lazy val actor = system.actorOf(Props(classOf[DbCommunicationActor], dbCommunicationConfig, "X", "Y"))
  }

  private trait testFsmActorScope extends actorScope {
    protected lazy val actor = TestFSMRef(new DbCommunicationActor(dbCommunicationConfig, "X", "Y"))
  }

  "DbCommunicationActor" should "report failed connection init when db config if wrong" in new realActorScope {
    val dbCommunicationConfig = DbCommunicationConfig("A", Some("B"), "C", "D", "E", None, None)
    actor ! InitDbConnection
    expectMsgPF(msgWaitTime) { case DbConnInitFinished(DbConnInitFailure(_, _, _)) =>}
  }

  it should "successfully initialize a connection when db config is correct" in new realActorScope {
    def dbCommunicationConfig =
      DbCommunicationConfig("jdbc:h2:mem://localhost", Some("org.h2.Driver"), "sa", "", "select 1", None, None)

    actor ! InitDbConnection
    expectMsgPF(msgWaitTime) { case DbConnInitFinished(DbConnInitSuccess(_, _)) =>}
  }

  it should "correctly report db call result" in new realActorScope {
    def dbCommunicationConfig: DbCommunicationConfig =
      DbCommunicationConfig("jdbc:h2:mem://localhost", Some("org.h2.Driver"), "sa", "", "select 1", None, None)

    actor ! InitDbConnection
    receiveOne(msgWaitTime)
    actor ! NextRound
    expectMsgPF(msgWaitTime) { case DbCallFinished(DbCallSuccess(_, _, _, FetchedRows(1))) =>}
  }

  it should "correctly report db call error when the query timeouts" in new realActorScope {
    def dbCommunicationConfig: DbCommunicationConfig = DbCommunicationConfig(
      "jdbc:h2:mem://localhost", Some("org.h2.Driver"), "sa", "",
      "CREATE ALIAS SLEEP1 FOR \"java.lang.Thread.sleep(long)\"; CALL SLEEP1(2000);", None, Some(1000))

    actor ! InitDbConnection
    receiveOne(msgWaitTime)
    actor ! NextRound
    expectMsgPF(msgWaitTime) { case DbCallFinished(DbCallFailure(_, _, _, e)) if e.isInstanceOf[TimeoutException] =>}
  }

  it should "report db call success when query takes some time, but doesn't timeout" in new realActorScope {

    val query: String = "CREATE ALIAS SLEEP2 FOR \"java.lang.Thread.sleep(long)\"; CALL SLEEP2(500);"
    val dbCommunicationConfig = DbCommunicationConfig("jdbc:h2:mem://localhost", Some("org.h2.Driver"), "sa", "", query, None, Some(1000))

    actor ! InitDbConnection
    receiveOne(msgWaitTime)
    actor ! NextRound
    expectMsgPF(msgWaitTime) { case DbCallFinished(DbCallSuccess(_, _, _, _)) =>}
  }

  it should "correctly replace the call ID placeholder" in new testFsmActorScope {

    val query = "@@gen_query_id@@ select 1 /* @@gen_query_id@@ */"
    val dbCommunicationConfig = DbCommunicationConfig("jdbc:h2:mem://localhost", Some("org.h2.Driver"), "sa", "", query, None, None)

    val connection = stub[Connection]
    val statement = stub[Statement]
    (connection.createStatement _).when().returns(statement).once()

    actor.setState(WaitForJob, Some(connection))

    actor ! NextRound

    actor ! PoisonPill
    (statement.execute(_: String)).verify(where { (x: String) =>
      val pattern = Pattern.compile("^X_Y_([a-zA-Z0-9]{4}) select 1 /\\* (X_Y_[a-zA-Z0-9]{4}) \\*/$")
      val matcher = pattern.matcher(x)
      matcher.matches() && {
        val generatedId = matcher.group(1)
        generatedId.length == 4 && x == s"X_Y_$generatedId select 1 /* X_Y_$generatedId */"
      }
    }).once()
  }

  it should "correctly close its initialized connection" in new testFsmActorScope {
    val dbCommunicationConfig = DbCommunicationConfig("A", None, "C", "D", "E", None, None)
    val connection = stub[Connection]
    actor.setState(stateData = Some(connection))

    actor ! PoisonPill

    (connection.close _).verify().once()
  }

}
