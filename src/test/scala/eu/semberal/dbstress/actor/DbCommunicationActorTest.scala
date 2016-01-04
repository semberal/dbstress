package eu.semberal.dbstress.actor

import java.sql.{Connection, Statement}
import java.util.regex.Pattern

import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.testkit.{TestActorRef, TestKit, TestKitBase}
import eu.semberal.dbstress.actor.DbCommunicationActor.NextRound
import eu.semberal.dbstress.model.Configuration.DbCommunicationConfig
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

class DbCommunicationActorTest extends TestKitBase with FlatSpecLike with Matchers with MockFactory with BeforeAndAfterAll {


  override implicit lazy val system: ActorSystem = ActorSystem()

  override protected def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  private trait actorScope {
    protected def dbCommunicationConfig: DbCommunicationConfig
    protected lazy val actor: TestActorRef[DbCommunicationActor] = TestActorRef(Props(classOf[DbCommunicationActor], dbCommunicationConfig, "X", "Y"))
  }

  "DbCommunicationActor" should "correctly close its initialized connection" in new actorScope {
    val dbCommunicationConfig = DbCommunicationConfig("A", None, "C", "D", "E", None, None)
    val connection = stub[Connection]

    actor.underlyingActor.connection = Some(connection)

    actor ! PoisonPill

    (connection.close _).verify().once()
  }

  it should "correctly replace the call ID placeholder" in new actorScope {

    val query = "@@gen_query_id@@ select 1 /* @@gen_query_id@@ */"
    val dbCommunicationConfig = DbCommunicationConfig("jdbc:h2:mem://localhost", Some("org.h2.Driver"), "sa", "", query, None, None)

    val connection = stub[Connection]
    val statement = stub[Statement]
    (connection.createStatement _).when().returns(statement)

    actor.underlyingActor.connection = Some(connection)
    actor.underlyingActor.context.become(actor.underlyingActor.waitForJob)

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

    (connection.close _).verify().once()

  }


}
