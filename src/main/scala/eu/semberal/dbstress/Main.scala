package eu.semberal.dbstress

import akka.actor.{ActorSystem, Props}
import com.typesafe.scalalogging.slf4j.LazyLogging
import eu.semberal.dbstress.actor.ManagerActor
import eu.semberal.dbstress.config.ConfigParser
import eu.semberal.dbstress.model.{DbConfig, Scenario, TestUnit, TestUnitConfig}

object Main extends LazyLogging {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem()
    val manager = system.actorOf(Props[ManagerActor], "manager")
    val sc = ConfigParser.parseConfigurationYaml()
    logger.debug("Starting scenario")
    manager ! sc
  }
}
