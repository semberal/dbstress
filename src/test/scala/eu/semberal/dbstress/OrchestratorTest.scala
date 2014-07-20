package eu.semberal.dbstress

import java.io.{File, InputStreamReader}
import java.nio.file.Files.createTempDirectory

import com.typesafe.scalalogging.slf4j.LazyLogging
import eu.semberal.dbstress.config.ConfigParser.parseConfigurationYaml
import grizzled.file.GrizzledFile._
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}
import play.api.libs.json.{JsArray, JsObject, Json}

import scala.concurrent.duration.DurationLong
import scala.io.Source

class OrchestratorTest extends FlatSpec with Matchers with BeforeAndAfter with LazyLogging {

  def withTempDir(testCode: File => Unit): Unit = {
    val file = createTempDirectory("dbstress_OrchestratorTest_").toFile
    try {
      testCode(file)
    } finally {
      file.deleteRecursively() match {
        case Left(msg) => logger.warn(s"Unable to delete temporary test directory ${file.getAbsolutePath}: $msg")
        case _ =>
      }
    }
  }

  "Orchestrator" should "successfully launch the application and check results" in withTempDir { tmpDir =>
    val reader = new InputStreamReader(getClass.getClassLoader.getResourceAsStream("config1.yaml"))
    val config = parseConfigurationYaml(reader).right.get
    val system = new Orchestrator(tmpDir).run(config)
    system.awaitTermination(5.seconds)

    val files = tmpDir.listFiles()
    files should have size 1
    val json = Json.parse(Source.fromFile(files(0)).mkString).asInstanceOf[JsObject]

    (json \ "scenarioResult" \ "unitResults").asInstanceOf[JsArray].value should have size 5
    (json \\ "configuration") should have size 5
    (json \\ "unitSummary") should have size 5
    (json \\ "connectionInit") should have size 108
  }
}

