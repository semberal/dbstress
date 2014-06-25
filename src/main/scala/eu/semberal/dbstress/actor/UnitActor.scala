package eu.semberal.dbstress.actor

import akka.actor._
import akka.event.LoggingReceive
import com.typesafe.scalalogging.slf4j.LazyLogging
import eu.semberal.dbstress.actor.ManagerActor.{UnitInitialized, UnitFinished}
import eu.semberal.dbstress.actor.UnitActor.{UnitRunFinished, StartUnit, InitUnit, UnitRunInitialized}
import eu.semberal.dbstress.actor.UnitRunActor.{StartUnitRun, InitUnitRun}
import eu.semberal.dbstress.model._

class UnitActor(unitConfig: UnitConfig) extends Actor with LazyLogging {

  override def receive: Receive = LoggingReceive {
    case InitUnit =>
      (1 to unitConfig.parallelConnections).map(i => {
        context.actorOf(Props(classOf[UnitRunActor], unitConfig.config), s"user${i}run") ! InitUnitRun
      })
      context.become(confirmationWait(unitConfig.parallelConnections))
  }

  private def unitRunResultWait(unitRunResults: List[UnitRunResult]): Receive = LoggingReceive {
    case UnitRunFinished(unitRunResult) =>
      logger.trace(s"Unit run result containing ${unitRunResult.size} items received")
      val newUnitRunResults: List[UnitRunResult] = unitRunResult :: unitRunResults
      if (newUnitRunResults.size == unitConfig.parallelConnections) {
        context.parent ! UnitFinished(UnitResult(unitConfig.name, newUnitRunResults))
      } else {
        context.become(unitRunResultWait(newUnitRunResults))
      }
  }

  private val startUnitWait: Receive = LoggingReceive {
    case StartUnit =>
      context.children.foreach(_ ! StartUnitRun)
      context.become(unitRunResultWait(Nil))
  }

  def confirmationWait(remainingConfirmations: Int): Receive = LoggingReceive {
    case UnitRunInitialized =>
      val newRemainingConfirmations = remainingConfirmations - 1
      if (newRemainingConfirmations > 0) {
        context.become(confirmationWait(newRemainingConfirmations))
      } else {
        context.parent ! UnitInitialized(unitConfig.name)
        context.become(startUnitWait)
      }
  }
}

object UnitActor {

  case object InitUnit

  case object UnitRunInitialized

  case object StartUnit

  case class UnitRunFinished(unitRunResult: UnitRunResult)

}
