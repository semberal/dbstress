package eu.semberal.dbstress

import akka.actor.{ActorSystem, Props}
import eu.semberal.dbstress.actor.ManagerActor
import eu.semberal.dbstress.model.{DbConfig, Scenario, TestUnit, TestUnitConfig}

object Main {
  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem()
    val manager = system.actorOf(Props[ManagerActor], "manager")
    val dbConfig1 = DbConfig("jdbc:h2:mem://localhost", "sa", "", "select 1")
    val uniConfig1 = TestUnitConfig(dbConfig1, 10000)
    val unit1 = TestUnit("unit1", uniConfig1, 4)
    val scenario = Scenario(List(unit1))
    manager ! scenario
  }
}
