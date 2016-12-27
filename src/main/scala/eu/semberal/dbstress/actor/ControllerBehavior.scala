package eu.semberal.dbstress.actor

import akka.typed.ActorContext
import akka.typed.ScalaDSL._
import akka.typed.{ActorRef, Behavior}
import eu.semberal.dbstress.Orchestrator.{ScenarioCompleteMsg, ScenarioFailure, ScenarioSuccess}
import eu.semberal.dbstress.actor.ControllerBehavior._
import eu.semberal.dbstress.actor.DatabaseBehavior.{DatabaseMsg, InitConnection, StartUnitRun}
import eu.semberal.dbstress.model.Configuration.ScenarioConfig
import eu.semberal.dbstress.model.Results._
import eu.semberal.dbstress.util.IdGen

class ControllerBehavior private(sc: ScenarioConfig) {

  private[this] val totalConnections = sc.units.map(_.parallelConnections).sum

  private val behaviorInit: Behavior[ControllerMsg] = ContextAware { ctx => behavior(ctx) }

  private[this] def behavior(ctx: ActorContext[ControllerMsg]): Behavior[ControllerMsg] = Partial {
    case RunScenario(replyTo) =>
      val scenarioId = IdGen.genScenarioId()
      val children = sc.units.flatMap(u => {
        (1 to u.parallelConnections).map { parId =>
          ctx.spawn(DatabaseBehavior(scenarioId, u.name, u.config), s"dbactor_${u.name}_$parId")
        }
      }).toList
      children.foreach(_ ! InitConnection(ctx.self))
      behavior2(ctx, replyTo, children, 0)

  }

  private[this] def behavior2(ctx: ActorContext[ControllerMsg],
                              replyTo: ActorRef[ScenarioCompleteMsg],
                              children: List[ActorRef[DatabaseMsg]],
                              collected: Int): Behavior[ControllerMsg] = Partial {
    case UnitRunInitializationFinished if collected + 1 == totalConnections =>

      children.foreach(_ ! StartUnitRun(ctx.self))
      behaviour3(ctx, replyTo, Nil)
    case UnitRunInitializationFinished =>
      behavior2(ctx, replyTo, children, collected + 1)
    case UnitRunInitializationFailed(e) =>
      replyTo ! ScenarioFailure(e)
      Stopped
  }


  private[this] def behaviour3(ctx: ActorContext[ControllerMsg],
                               replyTo: ActorRef[ScenarioCompleteMsg],
                               urResults: List[(String, UnitRunResult)]): Behavior[ControllerMsg] = Partial {
    case UnitRunFinished(unitName, result) =>
      val newUrResults = (unitName -> result) :: urResults
      if (totalConnections == newUrResults.length) {
        val unitResultMap = newUrResults.groupBy(_._1).mapValues(_.map(_._2))
        val sr = ScenarioResult(sc.units.map(conf => UnitResult(conf, unitResultMap(conf.name))).toList)
        replyTo ! ScenarioSuccess(sr)
        Stopped
      } else behaviour3(ctx, replyTo, newUrResults)
    case UnitRunError(e) =>
      replyTo ! ScenarioFailure(e)
      Stopped
  }
}

object ControllerBehavior {

  def apply(sc: ScenarioConfig): Behavior[ControllerMsg] = new ControllerBehavior(sc).behaviorInit

  sealed trait ControllerMsg

  case class RunScenario(replyTo: ActorRef[ScenarioCompleteMsg]) extends ControllerMsg

  private[actor] case object UnitRunInitializationFinished extends ControllerMsg

  private[actor] final case class UnitRunInitializationFailed(e: ConnectionInitException) extends ControllerMsg

  private[actor] final case class UnitRunFinished(unitName: String, result: UnitRunResult) extends ControllerMsg

  private[actor] final case class UnitRunError(e: UnitRunException) extends ControllerMsg

}