package eu.semberal.dbstress.service

import eu.semberal.dbstress.model.Results.ScenarioResult
import eu.semberal.dbstress.util.ResultsExport

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.Future._

class ExportingService(resultsExport: Seq[ResultsExport])(implicit
    ec: ExecutionContext
) {

  def runExport(sr: ScenarioResult): Future[Unit] = {
    sequence(resultsExport.map(x => Future(x.runExport(sr)))).map(_ => ())
  }

}
