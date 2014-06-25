package eu.semberal.dbstress

import akka.actor.{ActorSystem, Props}
import com.typesafe.scalalogging.slf4j.LazyLogging
import eu.semberal.dbstress.actor.ManagerActor
import eu.semberal.dbstress.actor.ManagerActor.RunScenario
import eu.semberal.dbstress.config.ConfigParser
import eu.semberal.dbstress.model.{DbCommunicationConfig, Scenario, UnitConfig, UnitRunConfig}

object Main extends LazyLogging {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem()
    val sc = ConfigParser.parseConfigurationYaml()
    val manager = system.actorOf(Props(classOf[ManagerActor], sc), "manager")
    logger.debug("Starting scenario")
    manager ! RunScenario
  }
}
