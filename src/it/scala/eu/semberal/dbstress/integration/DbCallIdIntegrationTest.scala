package eu.semberal.dbstress.integration

import org.scalatest.{FlatSpecLike, Matchers}
import play.api.libs.json.{JsObject, JsString, Json}

import scala.io.Source

class DbCallIdIntegrationTest extends FlatSpecLike with Matchers with AbstractDbstressIntegrationTest {

  "The application" should "correctly report database call IDs" in {
    val tmpDir = executeTest("config2.yaml")
    resource.managed(Source.fromFile(tmpDir.listFiles(jsonFilter).head)) foreach { source =>
      val json = Json.parse(source.mkString).asInstanceOf[JsObject]
      val values = (json \\ "callId").map(_.asInstanceOf[JsString].value.split("_").toList)
      values should have size 90
      values.map(_(0)).distinct should have size 1 // test run ID
      values.map(_(1)).distinct should have size 7 // connection ID
      values.map(_(2)).distinct should have size 90 // query ID
    }
  }
}
