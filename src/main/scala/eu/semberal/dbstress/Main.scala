package eu.semberal.dbstress

import akka.actor.{Props, ActorSystem}
import eu.semberal.dbstress.actor.ManagerActor
import eu.semberal.dbstress.model.{TestUnitConfig, TestUnit, Scenario}

object Main {
  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem()
    val manager = system.actorOf(Props[ManagerActor], "manager")
    val scenario = Scenario(List(TestUnit(TestUnitConfig("jdbc:h2:mem://localhost", "sa", "", "select 1"), 3)), 1)
    manager ! scenario

  }
}
