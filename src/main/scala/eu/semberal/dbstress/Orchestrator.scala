package eu.semberal.dbstress

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import eu.semberal.dbstress.actor.ControllerActor
import eu.semberal.dbstress.actor.ControllerActor.RunScenario
import eu.semberal.dbstress.model.Configuration.ScenarioConfig
import eu.semberal.dbstress.model.Results.ScenarioResult
import eu.semberal.dbstress.service.ExportingService
import eu.semberal.dbstress.util.ResultsExport

import scala.concurrent.Future

class Orchestrator(actorSystem: ActorSystem) extends LazyLogging {

  def run(sc: ScenarioConfig, exports: List[ResultsExport]): Future[Unit] = {

    val controller = actorSystem.actorOf(ControllerActor.props(sc), "controller")

    implicit val timeout: Timeout = Defaults.ScenarioTimeout

    implicit val executionContext = actorSystem.dispatcher

    val scenarioResultFuture = (controller ? RunScenario).mapTo[ScenarioResult]

    scenarioResultFuture.flatMap(sr => new ExportingService(exports).export(sr))
  }
}
