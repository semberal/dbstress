package eu.semberal.dbstress

import java.io.File

import akka.actor.{ActorSystem, Props}
import com.typesafe.scalalogging.slf4j.LazyLogging
import eu.semberal.dbstress.actor.ManagerActor.RunScenario
import eu.semberal.dbstress.actor.{ManagerActor, ResultsExporterActor, TerminatorActor}
import eu.semberal.dbstress.model.Configuration.ScenarioConfig

class Orchestrator(outputDir: File) extends LazyLogging {

  def run(sc: ScenarioConfig): ActorSystem = {
    val system = ActorSystem()
    val terminator = system.actorOf(Props[TerminatorActor], "terminator")
    val resultsExporter = system.actorOf(Props(classOf[ResultsExporterActor], outputDir), "resultsExporter")
    val manager = system.actorOf(Props(classOf[ManagerActor], sc, resultsExporter, terminator), "manager")
    logger.info("Starting the scenario")
    manager ! RunScenario
    system
  }
}
