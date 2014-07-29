package eu.semberal.dbstress

import java.io.{FilenameFilter, File, InputStreamReader}
import java.lang.System.currentTimeMillis
import java.nio.file.Files.createTempDirectory

import com.typesafe.scalalogging.slf4j.LazyLogging
import eu.semberal.dbstress.config.ConfigParser.parseConfigurationYaml
import grizzled.file.GrizzledFile._
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}
import play.api.libs.json.{JsArray, JsObject, Json}
import resource.managed

import scala.concurrent.duration.DurationLong
import scala.io.Source

class OrchestratorTest extends FlatSpec with Matchers with BeforeAndAfter with LazyLogging {

  def withTempDir(testCode: File => Unit): Unit = {
    val file = createTempDirectory(s"dbstress_OrchestratorTest_${currentTimeMillis()}_").toFile
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

    /* Test generated JSON */
    val jsonFiles = tmpDir.listFiles(new FilenameFilter {
      override def accept(dir: File, name: String): Boolean = name.endsWith(".json")
    })
    jsonFiles should have size 1

    managed(Source.fromFile(jsonFiles.head)) foreach { source =>
      val json = Json.parse(source.mkString).asInstanceOf[JsObject]
      (json \ "scenarioResult" \ "unitResults").asInstanceOf[JsArray].value should have size 5
      (json \\ "configuration") should have size 5
      (json \\ "unitSummary") should have size 5
      (json \\ "connectionInit") should have size 222
    }

    /* Test generated CSV file*/
    val csvFiles = tmpDir.listFiles(new FilenameFilter {
      override def accept(dir: File, name: String): Boolean = name.endsWith(".csv")
    })

    csvFiles should have size 1

    managed(Source.fromFile(csvFiles.head)) foreach { source =>
      val lines = source.getLines().toList
      lines should have size 6
      lines.tail.foreach { line =>
        line should startWith regex "\"unit[1-5]{1}\"".r
      }
    }
  }
}
