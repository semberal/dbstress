package eu.semberal.dbstress.integration

import akka.testkit.ImplicitSender
import org.scalatest.{FlatSpecLike, Matchers}
import resource.managed

import scala.io.Source

class OrchestratorIntegrationTest extends FlatSpecLike with Matchers with ImplicitSender with AbstractDbstressIntegrationTest {

  "Orchestrator" should "successfully launch the application and check results" in {
    val tmpDir = executeTest("config1.yaml")

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
