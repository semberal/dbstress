package eu.semberal.dbstress.integration

import akka.testkit.ImplicitSender
import org.scalatest.{FlatSpecLike, Matchers}
import resource.managed

import scala.io.Source

class OrchestratorIntegrationTest extends FlatSpecLike with Matchers with ImplicitSender with AbstractDbstressIntegrationTest {

  "Orchestrator" should "successfully launch the test of PostgreSQL and verify results" in {
    val tmpDir = executeTest("config.postgres.yaml")

    /* Test generated CSV file*/
    val csvFiles = tmpDir.listFiles(csvFilter)
    csvFiles should have size 1
    managed(Source.fromFile(csvFiles.head)) foreach { source =>
      val lines = source.getLines().toList
      lines should have size 3
      lines.tail.foreach { line =>
        line should startWith regex "unit[1-2]{1}".r
      }
    }
  }
}
