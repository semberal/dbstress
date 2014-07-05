package eu.semberal.dbstress.actor

import akka.actor.SupervisorStrategy.{Decider, Escalate, defaultDecider}
import akka.actor._
import com.typesafe.scalalogging.slf4j.LazyLogging
import eu.semberal.dbstress.actor.ManagerActor.{UnitFinished, UnitInitialized}
import eu.semberal.dbstress.actor.UnitActor._
import eu.semberal.dbstress.actor.UnitRunActor.{InitUnitRun, StartUnitRun}
import eu.semberal.dbstress.model.Configuration._
import eu.semberal.dbstress.model.Results._
import eu.semberal.dbstress.model._

class UnitActor(unitConfig: UnitConfig) extends Actor with LazyLogging with FSM[State, Data] {

  override def supervisorStrategy: SupervisorStrategy = {
    val decider: Decider = {
      case e: DbConnectionInitializationException => Escalate
    }
    OneForOneStrategy()(decider orElse defaultDecider)
  }

  startWith(Uninitialized, No)

  when(Uninitialized) {
    case Event(InitUnit, _) =>
      (1 to unitConfig.parallelConnections).map(i => {
        val actor = context.actorOf(Props(classOf[UnitRunActor], unitConfig.config), s"user${i}run")
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
        stay() // todo not elegant, work already done, should be stopped. Solve deadletters problem
      } else {
        goto(UnitRunResultsWait) using CollectedUnitRunResults(newUnitRunResults)
      }
  }

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

  protected sealed trait Data

  protected case object No extends Data

  protected case class RemainingInitUnitRunConfirmations(n: Int) extends Data

  protected case class CollectedUnitRunResults(l: List[UnitRunResult]) extends Data

}
