package eu.semberal.dbstress.integration

import java.io.{BufferedReader, InputStreamReader}
import java.lang.System._
import java.nio.file.Files._

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestKitBase}
import better.files._
import eu.semberal.dbstress.Orchestrator
import eu.semberal.dbstress.config.ConfigParser._
import eu.semberal.dbstress.util.{CsvResultsExport, ResultsExport}
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, Suite}

trait AbstractDbstressIntegrationTest extends TestKitBase with BeforeAndAfterAll with ScalaFutures {
  this: Suite =>

  override implicit lazy val system: ActorSystem = ActorSystem()

  override protected def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  protected def executeTest(configFile: String): File = {
    val tmpDir = createTempDirectory(s"dbstress_OrchestratorTest_${currentTimeMillis()}_").toFile.toScala

    val reader = new BufferedReader(new InputStreamReader(getClass.getClassLoader.getResourceAsStream(configFile))).autoClosed
    val config = parseConfigurationYaml(reader, Some(""))getOrElse(throw new IllegalStateException("Cannot parse configuration"))
    val exports: List[ResultsExport] = new CsvResultsExport(tmpDir) :: Nil
    whenReady(new Orchestrator(system).run(config, exports), Timeout(Span(10, Seconds)))(_ => tmpDir)
  }
}
