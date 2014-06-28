package eu.semberal.dbstress.actor

import akka.actor._
import eu.semberal.dbstress.actor.DbCommunicationActor.{InitDbConnection, NextRound}
import eu.semberal.dbstress.actor.UnitActor.{UnitRunFinished, UnitRunInitialized}
import eu.semberal.dbstress.actor.UnitRunActor._
import eu.semberal.dbstress.model._

class UnitRunActor(unitRunConfig: UnitRunConfig) extends Actor with ActorLogging with FSM[State, UnitRunResult] {

  private val DbCommunicationActorName = "dbCommunicationActor"

  startWith(Uninitialized, Nil)

  when(Uninitialized) {
    case Event(InitUnitRun, _) =>
      context.actorOf(Props(classOf[DbCommunicationActor], unitRunConfig.dbConfig), DbCommunicationActorName) ! InitDbConnection
      goto(ConfirmationWait)
  }

  when(ConfirmationWait) {
    case Event(DbConnectionInitialized, _) =>
      context.parent ! UnitRunInitialized
      goto(StartUnitRunWait)
  }

  when(StartUnitRunWait) {
    case Event(StartUnitRun, _) =>
      context.child(DbCommunicationActorName).foreach(_ ! NextRound)
      goto(ResultWait)
  }

  when(ResultWait) {
    case Event(DbCallFinished(dbResult), unitRunResult) =>
      val newUnitRunResult = dbResult :: unitRunResult

      if (newUnitRunResult.size < unitRunConfig.repeats) {
        context.child(DbCommunicationActorName).foreach(_ ! NextRound)
        stay() using newUnitRunResult
      } else {
        context.parent ! UnitRunFinished(newUnitRunResult)
        stop()
      }
  }

  initialize()
}

object UnitRunActor {

  case object InitUnitRun

  case object DbConnectionInitialized

  case object StartUnitRun

  case class DbCallFinished(dbResult: DbResult)

  case object DbConnectionTerminated

  protected sealed trait State

  protected case object Uninitialized extends State

  protected case object ConfirmationWait extends State

  protected case object StartUnitRunWait extends State

  protected case object ResultWait extends State

}
