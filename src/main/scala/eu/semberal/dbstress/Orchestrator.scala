package eu.semberal.dbstress

import akka.typed.ActorSystem
import akka.typed.AskPattern._
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import eu.semberal.dbstress.Orchestrator.{ScenarioCompleteMsg, ScenarioFailure, ScenarioSuccess}
import eu.semberal.dbstress.actor.ControllerBehavior.{ControllerMsg, RunScenario}
import eu.semberal.dbstress.model.Configuration.ScenarioConfig
import eu.semberal.dbstress.model.Results.ScenarioResult
import eu.semberal.dbstress.service.ExportingService
import eu.semberal.dbstress.util.ResultsExport

import scala.concurrent.Future

class Orchestrator(actorSystem: ActorSystem[ControllerMsg]) extends LazyLogging {

  def run(sc: ScenarioConfig, exports: List[ResultsExport]): Future[ScenarioCompleteMsg] = {

    implicit val timeout: Timeout = Defaults.ScenarioTimeout

    implicit val scheduler = actorSystem.scheduler

    val scenarioResultFuture = actorSystem ? RunScenario

    implicit val ec = actorSystem.executionContext

    scenarioResultFuture.flatMap {
      case x@ScenarioSuccess(sr) => new ExportingService(exports).export(sr).map(_ => x)
      case x: ScenarioFailure => Future.successful(x)
    }
  }
}

object Orchestrator {

  sealed trait ScenarioCompleteMsg

  final case class ScenarioSuccess(sr: ScenarioResult) extends ScenarioCompleteMsg

  final case class ScenarioFailure(e: Throwable) extends ScenarioCompleteMsg

}