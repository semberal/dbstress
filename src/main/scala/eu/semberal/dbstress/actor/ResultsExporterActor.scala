package eu.semberal.dbstress.actor

import java.io.File

import akka.actor.{Props, Actor, Status}
import eu.semberal.dbstress.actor.ManagerActor.ResultsExported
import eu.semberal.dbstress.actor.ResultsExporterActor.ExportResults
import eu.semberal.dbstress.model.Results.ScenarioResult
import eu.semberal.dbstress.util.{CsvResultsExport, JsonResultsExport, ResultsExport}

import scala.concurrent.Future
import scala.concurrent.Future.sequence
import scala.util.{Failure, Success}

class ResultsExporterActor(resultsExport: Seq[ResultsExport]) extends Actor {

  override def receive: Receive = {
    case ExportResults(sr: ScenarioResult) =>

      implicit val ec = context.system.dispatchers.lookup("akka.dispatchers.mgmt-dispatcher")

      val s = sender()

      sequence(resultsExport.map { x =>
        Future(x.export(sr))
      }) onComplete {
        case Success(_) => s ! ResultsExported
        case Failure(e) => s ! Status.Failure(e)
      }
  }
}

object ResultsExporterActor {

  def defaultProps(resultExports: List[ResultsExport]) = {
    Props(classOf[ResultsExporterActor], resultExports)
  }

  case class ExportResults(scenarioResult: ScenarioResult)

}
