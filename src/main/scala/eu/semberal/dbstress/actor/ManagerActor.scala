package eu.semberal.dbstress.actor

import java.util.concurrent.TimeUnit.MILLISECONDS

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.slf4j.LazyLogging
import eu.semberal.dbstress.Defaults.exportResultsTimeout
import eu.semberal.dbstress.actor.ManagerActor._
import eu.semberal.dbstress.actor.ResultsExporterActor.ExportResults
import eu.semberal.dbstress.actor.TerminatorActor.ScenarioCompleted
import eu.semberal.dbstress.actor.UnitActor.{InitUnit, StartUnit}
import eu.semberal.dbstress.model.Configuration._
import eu.semberal.dbstress.model.Results._
import eu.semberal.dbstress.util.IdGen
import eu.semberal.dbstress.util.IdGen.genScenarioId
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric

class ManagerActor(scenario: ScenarioConfig,
                   resultsExporter: ActorRef,
                   terminator: ActorRef) extends Actor with LazyLogging with LoggingFSM[State, Data] {

  startWith(Uninitialized, No)

  when(Uninitialized) {
    case Event(RunScenario, _) =>
      logger.info("Starting units initialization")
      val scenarioId = genScenarioId()
      scenario.units.foreach(u => {
        context.actorOf(Props(classOf[UnitActor], u), u.name) ! InitUnit(scenarioId)
      })
      goto(InitConfirmationsWait) using RemainingInitUnitConfirmations(scenario.units.length)
  }

  when(InitConfirmationsWait) {
    case Event(UnitInitialized(name), RemainingInitUnitConfirmations(n)) =>

      logger.info( s"""Unit "$name" initialization has finished, ${n - 1} more to go""")
      if (n == 1) {
        logger.info( s"""Initialization of all units has completed, starting the unit runs (db calls)""")
        context.children.foreach(_ ! StartUnit)
        goto(ResultWait) using CollectedUnitResults(Nil)
      } else {
        stay() using RemainingInitUnitConfirmations(n - 1)
      }
  }

  when(ResultWait) {
    case Event(UnitFinished(unitResult), CollectedUnitResults(l)) =>
      val newUnitResults: List[UnitResult] = unitResult :: l

      logger.info( s"""Unit "${unitResult.unitConfig.name}" has finished, ${scenario.units.size - newUnitResults.size} more to finish""")
      if (newUnitResults.size == scenario.units.size) {
        logger.info("All units have successfully finished")

        implicit val executionContext = context.system.dispatcher
        implicit val timeout = Timeout(exportResultsTimeout, MILLISECONDS)

        logger.info("Exporting the results")
        val exportFuture = resultsExporter ? ExportResults(ScenarioResult(newUnitResults))

        exportFuture.onSuccess { case _ => logger.info("Results have been successfully exported")}
        exportFuture.onFailure { case e => logger.error("An error while exporting results has occurred", e)}

        logger.info("Shutting down the actor system")
        exportFuture.onComplete(_ => terminator ! ScenarioCompleted)

        goto(TerminationWait) using No
      } else {
        stay() using CollectedUnitResults(newUnitResults)
      }
  }

  when(TerminationWait)(Map.empty) // just to make work transition to the TerminationWait state

  initialize()
}

object ManagerActor {

  case object RunScenario

  case object ResultsExported

  case class UnitInitialized(name: String)

  case class UnitFinished(unitResult: UnitResult)

  protected sealed trait State

  protected case object Uninitialized extends State

  protected case object InitConfirmationsWait extends State

  protected case object ResultWait extends State

  protected case object TerminationWait extends State

  protected sealed trait Data

  protected case object No extends Data

  protected case class RemainingInitUnitConfirmations(n: Int) extends Data

  protected case class CollectedUnitResults(l: List[UnitResult]) extends Data

}
