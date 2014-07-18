package eu.semberal.dbstress

import akka.actor.{ActorSystem, Props}
import com.typesafe.scalalogging.slf4j.LazyLogging
import eu.semberal.dbstress.Main._
import eu.semberal.dbstress.actor.ManagerActor.RunScenario
import eu.semberal.dbstress.actor.{ManagerActor, ResultsExporterActor, TerminatorActor}
import eu.semberal.dbstress.config.ConfigParser._
import eu.semberal.dbstress.model.Configuration.ScenarioConfig

class Orchestrator(config: CmdLineArguments) extends LazyLogging {

  def runScenario(): Unit = parseConfigurationYaml(config.configFile) match {
    case Left(msg) =>
      System.err.println(s"Configuration error: $msg")
      System.exit(2)
    case Right(sc) => run(sc)
  }

  private def run(sc: ScenarioConfig): Unit = {
    val system = ActorSystem()
    val terminator = system.actorOf(Props[TerminatorActor], "terminator")
    val resultsExporter = system.actorOf(Props(classOf[ResultsExporterActor], config.outputDir), "resultsExporter")
    val manager = system.actorOf(Props(classOf[ManagerActor], sc, resultsExporter, terminator), "manager")
    logger.info("Starting the scenario")
    manager ! RunScenario
  }
}
