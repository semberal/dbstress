package eu.semberal.dbstress.actor

import akka.actor._
import eu.semberal.dbstress.actor.DbCommunicationActor.{InitDbConnection, NextRound}
import eu.semberal.dbstress.actor.UnitActor.{UnitRunFinished, UnitRunInitialized}
import eu.semberal.dbstress.actor.UnitRunActor._
import eu.semberal.dbstress.model.Configuration._
import eu.semberal.dbstress.model.Results._

class UnitRunActor(unitRunConfig: UnitRunConfig) extends Actor with LoggingFSM[State, Option[UnitRunResult]] {

  private val DbCommunicationActorName = "dbCommunicationActor"

  startWith(Uninitialized, None)

  when(Uninitialized) {
    case Event(InitUnitRun, _) =>
      context.actorOf(Props(classOf[DbCommunicationActor], unitRunConfig.dbConfig), DbCommunicationActorName) ! InitDbConnection
      goto(ConfirmationWait)
  }

  when(ConfirmationWait) {
    case Event(DbConnInitFinished(r), _) => r match {
      case x: DbConnInitSuccess =>
        context.parent ! UnitRunInitialized
        goto(StartUnitRunWait) using Some(UnitRunResult(x, Nil))
      case x: DbConnInitFailure =>
        context.parent ! UnitRunInitialized
        goto(StartUnitRunWait) using Some(UnitRunResult(x, Nil))
    }
  }

  when(StartUnitRunWait) {
    case Event(StartUnitRun, Some(r@UnitRunResult(initResult, _))) => initResult match {
      case x: DbConnInitSuccess =>
        context.child(DbCommunicationActorName).foreach(_ ! NextRound)
        goto(ResultWait)
      case x: DbConnInitFailure =>
        context.parent ! UnitRunFinished(r)
        stop()
    }
  }

  when(ResultWait) {
    case Event(DbCallFinished(dbResult), Some(unitRunResult)) =>
      val newUnitRunResult = unitRunResult.copy(callResults = dbResult :: unitRunResult.callResults)

      if (newUnitRunResult.callResults.size < unitRunConfig.repeats) {
        context.child(DbCommunicationActorName).foreach(_ ! NextRound)
        stay() using Some(newUnitRunResult)
      } else {
        context.parent ! UnitRunFinished(newUnitRunResult)
        stop()
      }
  }

  initialize()
}

object UnitRunActor {

  case object InitUnitRun

  case class DbConnInitFinished(dbConnInitResult: DbConnInitResult)

  case object StartUnitRun

  case class DbCallFinished(dbResult: DbCallResult)

  case object DbConnectionTerminated

  protected sealed trait State

  protected case object Uninitialized extends State

  protected case object ConfirmationWait extends State

  protected case object StartUnitRunWait extends State

  protected case object ResultWait extends State

}
