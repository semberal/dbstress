package eu.semberal.dbstress.actor

import akka.actor.{Actor, Props}
import akka.event.LoggingReceive
import com.typesafe.scalalogging.slf4j.LazyLogging
import eu.semberal.dbstress.actor.ManagerActor.{UnitInitialized, UnitFinished, RunScenario}
import eu.semberal.dbstress.actor.UnitActor.{StartUnit, InitUnit}
import eu.semberal.dbstress.model.{Scenario, UnitResult}
import eu.semberal.dbstress.util.ResultsExporter

class ManagerActor(scenario: Scenario) extends Actor with LazyLogging with ResultsExporter {

  override def receive: Receive = LoggingReceive {
    case RunScenario =>
      scenario.units.foreach(u => {
        context.actorOf(Props(classOf[UnitActor], u), u.name) ! InitUnit
      })
      context.become(confirmationWait(scenario.units.size))
  }

  def confirmationWait(remainingConfirmations: Int): Receive = LoggingReceive {
    case UnitInitialized(unitName) =>
      val newRemainingConfirmations = remainingConfirmations - 1
      logger.info(s"""Unit "$unitName" has been initialized, $newRemainingConfirmations more to go}""")
      if(newRemainingConfirmations == 0) {
        logger.info("All units have been initialized, starting the test itself")
        context.children.foreach(_ ! StartUnit)
        context.become(resultWaitReceive(Nil))
      } else {
        context.become(confirmationWait(newRemainingConfirmations))
      }
  }

  def resultWaitReceive(unitResults: List[UnitResult]): Receive = LoggingReceive {
    case UnitFinished(unitResult) =>
      val newUnitResults: List[UnitResult] = unitResult :: unitResults
      logger.info(s"""Unit "${unitResult.name}" has finished, ${scenario.units.size - newUnitResults.size} more to finish""")
      if (newUnitResults.size == scenario.units.size) {
        logger.info("All units have successfully finished, exporting the results")
        exportResults("/home/semberal/Desktop", newUnitResults)
        logger.debug("Shutting down the actor system")
        context.system.shutdown()
      } else {
        context.become(resultWaitReceive(newUnitResults))
      }
  }
}

object ManagerActor {

  case object RunScenario

  case class UnitInitialized(name: String)

  case class UnitFinished(unitResult: UnitResult)

}
