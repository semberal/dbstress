package eu.semberal.dbstress.actor

import akka.actor._
import eu.semberal.dbstress.actor.ManagerActor.{UnitFinished, UnitInitialized}
import eu.semberal.dbstress.actor.UnitActor._
import eu.semberal.dbstress.actor.UnitRunActor.{InitUnitRun, StartUnitRun}
import eu.semberal.dbstress.model.Configuration._
import eu.semberal.dbstress.model.Results._

class UnitActor(unitConfig: UnitConfig) extends Actor {

  override def receive = {
    case InitUnit(scenarioId) =>
      (1 to unitConfig.parallelConnections).foreach(i => {
        val actor = context.actorOf(Props(classOf[UnitRunActor], unitConfig.config), s"run$i")
        actor ! InitUnitRun(scenarioId)
      })
      context.become(confirmationWait(unitConfig.parallelConnections))
  }

  private def confirmationWait(remaining: Int): Receive = {
    case UnitRunInitialized =>
      if (remaining == 1) {
        context.parent ! UnitInitialized(unitConfig.name)
        context.become(startUnitWait)
      } else context.become(confirmationWait(remaining - 1))
  }

  private def startUnitWait: Receive = {
    case StartUnit =>
      context.children.foreach(_ ! StartUnitRun)
      context.become(unitRunResultsWait(Nil))
  }

  private def unitRunResultsWait(alreadyCollected: List[UnitRunResult]): Receive = {
    case UnitRunFinished(result) =>
      val newAlreadyCollected = result :: alreadyCollected
      if(newAlreadyCollected.length == unitConfig.parallelConnections) {
        context.parent ! UnitFinished(UnitResult(unitConfig, newAlreadyCollected.reverse))
        self ! PoisonPill
      } else context.become(unitRunResultsWait(newAlreadyCollected))
  }
}

object UnitActor {

  case class InitUnit(scenarioId: String)

  case object UnitRunInitialized

  case object StartUnit

  case class UnitRunFinished(unitRunResult: UnitRunResult)

}
