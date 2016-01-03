package eu.semberal.dbstress.actor

import akka.actor._
import com.typesafe.scalalogging.slf4j.LazyLogging
import eu.semberal.dbstress.actor.ManagerActor._
import eu.semberal.dbstress.actor.UnitActor.{InitUnit, StartUnit}
import eu.semberal.dbstress.model.Configuration._
import eu.semberal.dbstress.model.Results._
import eu.semberal.dbstress.util.IdGen.genScenarioId

class ManagerActor(scenario: ScenarioConfig) extends Actor with LazyLogging {

  private var originalSender: Option[ActorRef] = None

  override def receive: Receive = unintialized

  def unintialized: Receive = {
    case RunScenario =>
      originalSender = Some(sender())
      logger.info("Starting units initialization")
      val scenarioId = genScenarioId()
      scenario.units.foreach(u => {
        context.actorOf(Props(classOf[UnitActor], u), u.name) ! InitUnit(scenarioId)
      })
      context.become(initConfirmationWait(scenario.units.length))
  }

  def initConfirmationWait(remaining: Int): Receive = {
    case UnitInitialized(name) =>
      logger.info( s"""Unit "$name" initialization has finished, ${remaining - 1} more to go""")
      if (remaining == 1) {
        logger.info( s"""Initialization of all units has completed, starting the unit runs (db calls)""")
        context.children.foreach(_ ! StartUnit)
        context.become(resultWait(Nil))
      } else {
        context.become(initConfirmationWait(remaining - 1))
      }
  }

  def resultWait(results: List[UnitResult]): Receive = {
    case UnitFinished(unitResult) =>
      val newUnitResults: List[UnitResult] = unitResult :: results
      logger.info( s"""Unit "${unitResult.unitConfig.name}" has finished, ${scenario.units.size - newUnitResults.size} more to finish""")
      if (newUnitResults.size == scenario.units.size) {
        logger.info("All units have successfully finished")
        originalSender.foreach(_ ! ScenarioResult(newUnitResults))
        self ! PoisonPill
      } else {
        context.become(resultWait(newUnitResults))
      }
  }

}

object ManagerActor {

  case object RunScenario

  final case class UnitInitialized(name: String)

  final case class UnitFinished(unitResult: UnitResult)

}
