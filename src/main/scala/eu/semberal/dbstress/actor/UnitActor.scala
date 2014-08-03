package eu.semberal.dbstress.actor

import akka.actor._
import eu.semberal.dbstress.actor.ManagerActor.{UnitFinished, UnitInitialized}
import eu.semberal.dbstress.actor.UnitActor._
import eu.semberal.dbstress.actor.UnitRunActor.{InitUnitRun, StartUnitRun}
import eu.semberal.dbstress.model.Configuration._
import eu.semberal.dbstress.model.Results._

class UnitActor(unitConfig: UnitConfig) extends Actor with LoggingFSM[State, Data] {

  startWith(Uninitialized, No)

  when(Uninitialized) {
    case Event(InitUnit, _) =>
      (1 to unitConfig.parallelConnections).map(i => {
        val actor = context.actorOf(Props(classOf[UnitRunActor], unitConfig.config), s"run$i")
        actor ! InitUnitRun
      })
      goto(InitConfirmationsWait) using RemainingInitUnitRunConfirmations(unitConfig.parallelConnections)
  }

  when(InitConfirmationsWait) {
    case Event(UnitRunInitialized, RemainingInitUnitRunConfirmations(n)) =>
      if (n == 1) {
        context.parent ! UnitInitialized(unitConfig.name)
        goto(StartUnitWait) using No
      } else {
        goto(InitConfirmationsWait) using RemainingInitUnitRunConfirmations(n - 1)
      }
  }

  when(StartUnitWait) {
    case Event(StartUnit, _) =>
      context.children.foreach(_ ! StartUnitRun)
      goto(UnitRunResultsWait) using CollectedUnitRunResults(Nil)
  }

  when(UnitRunResultsWait) {
    case Event(UnitRunFinished(result), CollectedUnitRunResults(l)) =>
      val newUnitRunResults = result :: l
      if (newUnitRunResults.length == unitConfig.parallelConnections) {
        context.parent ! UnitFinished(UnitResult(unitConfig, newUnitRunResults))
        goto(TerminationWait) using No
      } else {
        goto(UnitRunResultsWait) using CollectedUnitRunResults(newUnitRunResults)
      }
  }

  when(TerminationWait)(Map.empty) // just to make work transition to the TerminationWait state

  initialize()
}

object UnitActor {

  case object InitUnit

  case object UnitRunInitialized

  case object StartUnit

  case class UnitRunFinished(unitRunResult: UnitRunResult)

  protected sealed trait State

  protected case object Uninitialized extends State

  protected case object InitConfirmationsWait extends State

  protected case object StartUnitWait extends State

  protected case object UnitRunResultsWait extends State

  protected case object TerminationWait extends State

  protected sealed trait Data

  protected case object No extends Data

  protected case class RemainingInitUnitRunConfirmations(n: Int) extends Data

  protected case class CollectedUnitRunResults(l: List[UnitRunResult]) extends Data

}
