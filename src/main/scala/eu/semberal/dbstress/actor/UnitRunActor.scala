package eu.semberal.dbstress.actor

import akka.actor._
import eu.semberal.dbstress.actor.DbCommunicationActor.{InitDbConnection, NextRound}
import eu.semberal.dbstress.actor.UnitActor.{UnitRunFinished, UnitRunInitialized}
import eu.semberal.dbstress.actor.UnitRunActor._
import eu.semberal.dbstress.model.Configuration._
import eu.semberal.dbstress.model.Results._
import eu.semberal.dbstress.util.IdGen.genStatementId

class UnitRunActor(unitRunConfig: UnitRunConfig) extends Actor {

  private val DbCommunicationActorName = "dbCommunicationActor"

  override def receive = {
    case InitUnitRun(scenarioId) =>
      context.actorOf(Props(classOf[DbCommunicationActor], unitRunConfig.dbConfig, scenarioId, genStatementId()), DbCommunicationActorName) ! InitDbConnection
      context.become(confirmationWait)
  }

  private def confirmationWait: Receive = {
    case DbConnInitFinished(r) => r match {
      case x: DbConnInitResult =>
        context.parent ! UnitRunInitialized
        context.become(startUnitRunWait(x))
    }
  }

  private def startUnitRunWait(connInitResult: DbConnInitResult): Receive = {
    case StartUnitRun =>
      val unitRunResult = UnitRunResult(connInitResult, Nil)
      connInitResult match {
        case x: DbConnInitSuccess =>
          context.child(DbCommunicationActorName).foreach(_ ! NextRound)
          context.become(dbCallResultWait(unitRunResult))
        case x: DbConnInitFailure =>
          context.parent ! UnitRunFinished(unitRunResult)
          self ! PoisonPill
      }
  }

  private def dbCallResultWait(resultSoFar: UnitRunResult): Receive = {
    case DbCallFinished(dbCallResult) =>
      val newResultSoFar = resultSoFar.copy(callResults = dbCallResult :: resultSoFar.callResults)
      if(newResultSoFar.callResults.size == unitRunConfig.repeats) {
        context.parent ! UnitRunFinished(newResultSoFar)
        self ! PoisonPill
      } else {
        context.child(DbCommunicationActorName).foreach(_ ! NextRound)
        context.become(dbCallResultWait(newResultSoFar))
      }
  }
}

object UnitRunActor {

  case class InitUnitRun(scenarioId: String)

  case class DbConnInitFinished(dbConnInitResult: DbConnInitResult)

  case object StartUnitRun

  case class DbCallFinished(dbResult: DbCallResult)

}
