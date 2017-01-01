package eu.semberal.dbstress.actor

import akka.actor.{Actor, ActorRef, Props, Status}
import akka.event.LoggingReceive
import com.typesafe.scalalogging.LazyLogging
import eu.semberal.dbstress.actor.ControllerActor._
import eu.semberal.dbstress.actor.DatabaseActor.{InitConnection, StartUnitRun}
import eu.semberal.dbstress.model.Configuration.ScenarioConfig
import eu.semberal.dbstress.model.Results._
import eu.semberal.dbstress.util.IdGen

class ControllerActor(sc: ScenarioConfig) extends Actor with LazyLogging {
  private val totalConnections = sc.units.map(_.parallelConnections).sum

  override def receive: Receive = LoggingReceive {
    case RunScenario =>
      val scenarioId = IdGen.genScenarioId()
      logger.info(s"Starting the scenario (ID: $scenarioId)")

      sc.units.foreach(u => {
        (1 to u.parallelConnections).foreach { parId =>
          val a = context.actorOf(DatabaseActor.props(scenarioId, u.name, u.config), s"dbactor_${u.name}_$parId")
          a ! InitConnection
        }
      })
      context.become(waitForInitConfirmation(sender(), 0))
      logger.info(s"Waiting for $totalConnections database connections to be initialized")
  }

  private def waitForInitConfirmation(client: ActorRef, collectedCount: Int): Receive = LoggingReceive {

    case UnitRunInitializationFinished if collectedCount + 1 == totalConnections =>
      logger.info("All database connections initialized, proceeding to the query execution phase")
      context.children.foreach { ref =>
        ref ! StartUnitRun
      }
      context.become(waitForFinish(client, Nil))

    case UnitRunInitializationFinished =>
      logger.debug(s"Initialized database connections: ${collectedCount + 1}/$totalConnections")
      context.become(waitForInitConfirmation(client, collectedCount + 1))

    case UnitRunInitializationFailed(e) =>
      val msg = e match {
        case _: ClassNotFoundException => s"Class ${e.getMessage} not found"
        case _ => e.getMessage
      }

      logger.error(s"Cannot initialize database connection: $msg")
      context.stop(self)
      client ! Status.Failure(new ConnectionInitException(e))
  }

  private def waitForFinish(client: ActorRef, urResults: List[(String, UnitRunResult)]): Receive = LoggingReceive {
    case UnitRunFinished(unitName, result) =>
      logger.info(s"Finished unit runs: ${urResults.length + 1}/$totalConnections")
      context.become(waitForFinish(client, (unitName -> result) :: urResults))
      if (totalConnections - urResults.length == 1) self ! Done

    case UnitRunError(e) =>
      context.stop(self)
      client ! Status.Failure(e)

    case Done =>
      val allCalls = urResults.flatMap(_._2.callResults)
      val failedCalls = allCalls.count(_.isInstanceOf[DbCallFailure])
      val msg = if (failedCalls > 0) s"($failedCalls/${allCalls.length} calls failed)" else ""
      logger.info("All database operations finished {}", msg)
      val unitResultMap = urResults.groupBy(_._1).mapValues(_.map(_._2))
      client ! ScenarioResult(sc.units.map(conf => UnitResult(conf, unitResultMap(conf.name))).toList)
  }
}

object ControllerActor {

  case object RunScenario

  private[actor] case object UnitRunInitializationFinished

  private[actor] final case class UnitRunInitializationFailed(e: Throwable)

  private[actor] final case class UnitRunFinished(unitName: String, result: UnitRunResult)

  private[actor] final case class UnitRunError(e: UnitRunException)

  private case object Done

  def props(sc: ScenarioConfig) = Props(classOf[ControllerActor], sc)
}
