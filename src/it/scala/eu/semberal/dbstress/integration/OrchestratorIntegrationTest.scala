package eu.semberal.dbstress.integration

import akka.testkit.ImplicitSender
import org.scalatest.flatspec.AnyFlatSpecLike

class OrchestratorIntegrationTest
    extends AnyFlatSpecLike
    with ImplicitSender
    with AbstractDbstressIntegrationTest {

  "Orchestrator" should "successfully launch the test of PostgreSQL and verify results" in {
    val tmpDir = executeTest("config.postgres.yaml")

    val csvFiles = tmpDir.glob("*.csv").toList
    assert(csvFiles.length === 1)

    val lines = csvFiles.head.lines
    assert(lines.size === 3)

    lines.tail.foreach { line =>
      assert(line.matches("^unit[1-2].*$"))
    }
  }
}
