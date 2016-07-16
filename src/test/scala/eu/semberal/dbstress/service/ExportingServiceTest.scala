package eu.semberal.dbstress.service

import java.io.File

import eu.semberal.dbstress.model.Results.ScenarioResult
import eu.semberal.dbstress.util.{CsvResultsExport, JsonResultsExport, ResultsExport}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpecLike, Matchers}
import scala.concurrent.ExecutionContext.Implicits.global

class ExportingServiceTest extends FlatSpecLike with Matchers with ScalaFutures {

  "ResultsExporterActor" should "respond with exception when an error occurs" in {

    val outputDir: File = new File("/root")
    val exports: List[ResultsExport] = new JsonResultsExport(outputDir) :: new CsvResultsExport(outputDir) :: Nil
    val service = new ExportingService(exports)(global) // IDEA is messing up imports here

    service.export(ScenarioResult(Nil))

    whenReady(service.export(ScenarioResult(Nil)).failed) {
      _.getMessage should fullyMatch regex "^/root/complete\\.[0-9]{8}_[0-9]{6}\\.json \\(.*\\)$".r
    }
  }
}
