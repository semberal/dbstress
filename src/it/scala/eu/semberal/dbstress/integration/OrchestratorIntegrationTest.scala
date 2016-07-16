package eu.semberal.dbstress.integration

import akka.testkit.ImplicitSender
import org.scalatest.{FlatSpecLike, Matchers}
import play.api.libs.json.{JsArray, JsNumber, JsObject, Json}
import resource.managed

import scala.io.Source

class OrchestratorIntegrationTest extends FlatSpecLike with Matchers with ImplicitSender with AbstractDbstressIntegrationTest {

  "Orchestrator" should "successfully launch the application and check results" in {
    val tmpDir = executeTest("config1.yaml")
    /* Test generated JSON */
    val jsonFiles = tmpDir.listFiles(jsonFilter)
    jsonFiles should have size 1

    managed(Source.fromFile(jsonFiles.head)) foreach { source =>
      val json = Json.parse(source.mkString).asInstanceOf[JsObject]
      val unitResults = (json \ "scenarioResult" \ "unitResults").get.asInstanceOf[JsArray].value.map(_.as[JsObject])
      unitResults should have size 6
      (json \\ "configuration") should have size 6
      (json \\ "unitSummary") should have size 6
      (json \\ "connectionInit") should have size 60

      val m = unitResults.map(x =>
        (x \ "configuration" \ "name").as[String] -> (x \ "unitSummary").as[JsObject]
      ).toMap

      (m("unit1") \ "expectedDbCalls").get should be(JsNumber(540))
      (m("unit1") \ "dbCalls" \ "executed" \ "count").get should be(JsNumber(540))
      (m("unit1") \ "dbCalls" \ "executed" \ "successful" \ "count").get should be(JsNumber(540))
    }

    /* Test generated CSV file*/
    val csvFiles = tmpDir.listFiles(csvFilter)
    csvFiles should have size 1
    managed(Source.fromFile(csvFiles.head)) foreach { source =>
      val lines = source.getLines().toList
      lines should have size 7
      lines.tail.foreach { line =>
        line should startWith regex "\"unit[1-6]{1}\"".r
      }
    }
  }
}
