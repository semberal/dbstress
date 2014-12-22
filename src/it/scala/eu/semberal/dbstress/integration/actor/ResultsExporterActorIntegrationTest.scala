package eu.semberal.dbstress.integration.actor

import java.io.File

import akka.actor.Status
import akka.pattern.ask
import akka.testkit.ImplicitSender
import akka.util.Timeout
import eu.semberal.dbstress.actor.ResultsExporterActor
import eu.semberal.dbstress.actor.ResultsExporterActor.ExportResults
import eu.semberal.dbstress.integration.AbstractDbstressIntegrationTest
import eu.semberal.dbstress.model.Results.ScenarioResult
import eu.semberal.dbstress.util.{CsvResultsExport, JsonResultsExport, ResultsExport}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpecLike, Matchers}

import scala.concurrent.duration.DurationLong

class ResultsExporterActorIntegrationTest extends FlatSpecLike with Matchers with ImplicitSender with ScalaFutures with AbstractDbstressIntegrationTest {

  private trait failedActorScope {
    val outputDir: File = new File("/root")
    val exports: List[ResultsExport] = new JsonResultsExport(outputDir) :: new CsvResultsExport(outputDir) :: Nil
    val actor = system.actorOf(ResultsExporterActor.defaultProps(exports))
  }

  "ResultsExporterActor" should "respond with exception when an error occurs" in new failedActorScope {
    actor ! ExportResults(ScenarioResult(Nil))
    expectMsgPF(1.second) {
      case Status.Failure(e) =>
        e.getMessage should fullyMatch regex "^/root/complete\\.[0-9]{8}_[0-9]{6}\\.json \\(Permission denied\\)$".r
    }
  }

  it should "produce a failed future when asked" in new failedActorScope {
    implicit val timeout = Timeout(1.second)
    val f = actor ? ExportResults(ScenarioResult(Nil))
    whenReady(f.failed) { ex =>
      ex.getMessage should fullyMatch regex "^/root/complete\\.[0-9]{8}_[0-9]{6}\\.json \\(Permission denied\\)$".r
    }
  }
}
