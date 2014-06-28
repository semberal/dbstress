package eu.semberal.dbstress.actor

import akka.actor.{ActorRef, Actor, FSM, Props}
import com.typesafe.scalalogging.slf4j.LazyLogging
import eu.semberal.dbstress.actor.ManagerActor._
import eu.semberal.dbstress.actor.TerminatorActor.ScenarioCompleted
import eu.semberal.dbstress.actor.UnitActor.{InitUnit, StartUnit}
import eu.semberal.dbstress.model.{Scenario, UnitResult}
import eu.semberal.dbstress.util.ResultsExporter

class ManagerActor(scenario: Scenario, terminator: ActorRef) extends Actor with LazyLogging with ResultsExporter with FSM[State, Data] {

  startWith(Uninitialized, No)

  when(Uninitialized) {
    case Event(RunScenario, _) =>
      scenario.units.foreach(u => {
        context.actorOf(Props(classOf[UnitActor], u), u.name) ! InitUnit
      })
      goto(InitConfirmationsWait) using RemainingInitUnitConfirmations(scenario.units.length)
  }

  when(InitConfirmationsWait) {
    case Event(UnitInitialized(_), RemainingInitUnitConfirmations(n)) =>
      if (n == 1) {
        context.children.foreach(_ ! StartUnit)
        goto(ResultWait) using CollectedUnitResults(Nil)
      } else {
        stay() using RemainingInitUnitConfirmations(n - 1)
      }
  }

  when(ResultWait) {
    case Event(UnitFinished(unitResult), CollectedUnitResults(l)) =>
      val newUnitResults: List[UnitResult] = unitResult :: l
      logger.info( s"""Unit "${unitResult.name}" has finished, ${scenario.units.size - newUnitResults.size} more to finish""")
      if (newUnitResults.size == scenario.units.size) {
        logger.info("All units have successfully finished, exporting the results")
        exportResults("/home/semberal/Desktop", newUnitResults) // todo path from config
        terminator ! ScenarioCompleted
        stay() // todo stay here?
      } else {
        stay() using CollectedUnitResults(newUnitResults)
      }
  }

  initialize()
}

object ManagerActor {

  case object RunScenario

  case class UnitInitialized(name: String)

  case class UnitFinished(unitResult: UnitResult)

  protected sealed trait State

  protected case object Uninitialized extends State

  protected case object InitConfirmationsWait extends State

  protected case object ResultWait extends State

  protected sealed trait Data

  protected case object No extends Data

  protected case class RemainingInitUnitConfirmations(n: Int) extends Data

  protected case class CollectedUnitResults(l: List[UnitResult]) extends Data

}
