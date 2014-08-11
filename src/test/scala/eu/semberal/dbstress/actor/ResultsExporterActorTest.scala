package eu.semberal.dbstress.actor

import java.io.File

import akka.actor.{ActorSystem, Status}
import akka.pattern.ask
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import eu.semberal.dbstress.actor.ResultsExporterActor.ExportResults
import eu.semberal.dbstress.model.Results.ScenarioResult
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.duration.DurationLong

class ResultsExporterActorTest
  extends TestKit(ActorSystem())
  with FlatSpecLike
  with Matchers
  with ImplicitSender
  with BeforeAndAfterAll with ScalaFutures {

  override protected def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  trait failedActorScope {
    val actor = system.actorOf(ResultsExporterActor.defaultProps(new File("/root")))
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
