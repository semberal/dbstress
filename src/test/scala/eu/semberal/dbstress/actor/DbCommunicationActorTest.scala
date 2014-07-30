package eu.semberal.dbstress.actor

import java.sql.Connection

import akka.actor.{PoisonPill, Props, ActorSystem}
import akka.testkit.{ImplicitSender, TestFSMRef, TestKit}
import eu.semberal.dbstress.actor.DbCommunicationActor.{NextRound, InitDbConnection}
import eu.semberal.dbstress.actor.UnitRunActor.{DbCallFinished, DbConnInitFinished}
import eu.semberal.dbstress.model.Configuration.DbCommunicationConfig
import eu.semberal.dbstress.model.Results.{FetchedRows, DbCallSuccess, DbConnInitSuccess, DbConnInitFailure}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.duration.DurationDouble

class DbCommunicationActorTest
  extends TestKit(ActorSystem())
  with FlatSpecLike
  with Matchers
  with ImplicitSender
  with BeforeAndAfterAll
  with MockFactory {


  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  trait dbCommunicationConfigScope {
    val dbCommunicationConfig: DbCommunicationConfig
  }

  trait correctDbCommunicationConfigScope extends dbCommunicationConfigScope {
    val dbCommunicationConfig: DbCommunicationConfig =
      DbCommunicationConfig("jdbc:h2:mem://localhost", Some("org.h2.Driver"), "sa", "", "select 1", Some(1000), Some(1000))
  }

  trait wrongDbCommunicationConfigScope extends dbCommunicationConfigScope {
    val dbCommunicationConfig = DbCommunicationConfig("A", Some("B"), "C", "D", "E", Some(1), Some(2))
  }

  trait actorScope {
    this: dbCommunicationConfigScope =>
    val actor = system.actorOf(Props(classOf[DbCommunicationActor], dbCommunicationConfig))
  }

  "DbCommunicationActor" should "report failed connection init when db config if wrong" in
    new wrongDbCommunicationConfigScope with actorScope {
      actor ! InitDbConnection
      expectMsgPF(1.second) { case DbConnInitFinished(DbConnInitFailure(_, _, _)) =>}
    }

  it should "successfully initialize a connection when db config is correct" in
    new correctDbCommunicationConfigScope with actorScope {

      actor ! InitDbConnection
      expectMsgPF(1.second) { case DbConnInitFinished(DbConnInitSuccess(_, _)) =>}
    }

  it should "correctly report db call results" in
    new correctDbCommunicationConfigScope with actorScope {
      actor ! InitDbConnection
      receiveOne(1.second)
      actor ! NextRound
      expectMsgPF(1.seconds) { case DbCallFinished(DbCallSuccess(_, _, FetchedRows(1))) =>}
    }

  it should "correctly close its initialized connection" in {
    val dbCommunicationConfig = DbCommunicationConfig("A", Some("B"), "C", "D", "E", Some(1), Some(2))
    val actor = TestFSMRef(new DbCommunicationActor(dbCommunicationConfig))
    val connection = stub[Connection]
    actor.setState(stateData = Some(connection))

    actor ! PoisonPill

    (connection.close _).verify().once()
  }
}
