package eu.semberal.dbstress

import java.io.File

import akka.actor.{ActorSystem, Props}
import com.typesafe.scalalogging.slf4j.LazyLogging
import eu.semberal.dbstress.actor.ManagerActor.RunScenario
import eu.semberal.dbstress.actor.{ManagerActor, ResultsExporterActor, TerminatorActor}
import eu.semberal.dbstress.model.Configuration.ScenarioConfig
import eu.semberal.dbstress.util.{ResultsExport, CsvResultsExport, JsonResultsExport}

class Orchestrator(exports: List[ResultsExport]) extends LazyLogging {

  def run(sc: ScenarioConfig, actorSystem: ActorSystem): Unit = {
    val terminator = actorSystem.actorOf(Props[TerminatorActor], "terminator")
    val resultsExporter = actorSystem.actorOf(ResultsExporterActor.defaultProps(exports), "resultsExporter")
    val manager = actorSystem.actorOf(Props(classOf[ManagerActor], sc, resultsExporter, terminator), "manager")
    logger.info("Starting the scenario")
    manager ! RunScenario
  }
}
