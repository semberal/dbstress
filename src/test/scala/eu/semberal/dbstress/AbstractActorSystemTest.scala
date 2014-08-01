package eu.semberal.dbstress

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

abstract class AbstractActorSystemTest
  extends TestKit(ActorSystem())
  with FlatSpecLike
  with Matchers
  with ImplicitSender
  with BeforeAndAfterAll {

  override protected def afterAll(): Unit = TestKit.shutdownActorSystem(system)

}
