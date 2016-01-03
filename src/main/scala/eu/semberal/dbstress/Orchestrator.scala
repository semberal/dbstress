package eu.semberal.dbstress

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.slf4j.LazyLogging
import eu.semberal.dbstress.actor.ManagerActor
import eu.semberal.dbstress.actor.ManagerActor.RunScenario
import eu.semberal.dbstress.model.Configuration.ScenarioConfig
import eu.semberal.dbstress.model.Results.ScenarioResult
import eu.semberal.dbstress.service.ExportingService
import eu.semberal.dbstress.util.ResultsExport

import scala.concurrent.Future

class Orchestrator(actorSystem: ActorSystem) extends LazyLogging {

  def run(sc: ScenarioConfig, exports: List[ResultsExport]): Future[Unit] = {

    val manager = actorSystem.actorOf(Props(classOf[ManagerActor], sc), "manager")

    implicit val timeout: Timeout = Defaults.ScenarioTimeout

    implicit val executionContext = actorSystem.dispatcher // todo review dispatchers

    val scenarioResultFuture = (manager ? RunScenario).mapTo[ScenarioResult]
    scenarioResultFuture.flatMap(sr => new ExportingService(exports).export(sr))
  }
}
