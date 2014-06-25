package eu.semberal.dbstress.actor

import akka.actor._
import akka.event.LoggingReceive
import eu.semberal.dbstress.actor.DbCommunicationActor.{InitDbConnection, NextRound, CloseConnection}
import eu.semberal.dbstress.actor.UnitActor.{UnitRunFinished, UnitRunInitialized}
import eu.semberal.dbstress.actor.UnitRunActor.{DbCallFinished, DbConnectionInitialized, InitUnitRun, StartUnitRun}
import eu.semberal.dbstress.model._

class UnitRunActor(unitRunConfig: UnitRunConfig) extends Actor with ActorLogging {

  private val DbCommunicationActorName = "dbCommunicationActor"

  override def receive = LoggingReceive {
    case InitUnitRun =>
      context.become(confirmationWait)
      val dbWorker = context.actorOf(Props(classOf[DbCommunicationActor], unitRunConfig.dbConfig), DbCommunicationActorName)
      dbWorker ! InitDbConnection
  }

  private val startUnitRunWait: Receive = LoggingReceive {
    case StartUnitRun =>
      context.child(DbCommunicationActorName).foreach(_ ! NextRound)
      context.become(resultWait(Nil))
  }

  private val confirmationWait: Receive = LoggingReceive {
    case DbConnectionInitialized =>
      context.parent ! UnitRunInitialized
      context.become(startUnitRunWait)
  }

  def resultWait(result: UnitRunResult): Receive = LoggingReceive {
    case DbCallFinished(dbResult) =>
      val newResult = dbResult :: result

      if (newResult.size < unitRunConfig.repeats) {
        context.child(DbCommunicationActorName).foreach(_ ! NextRound)
        context.become(resultWait(newResult))
      } else {
        context.parent ! UnitRunFinished(newResult)
        context.child(DbCommunicationActorName).foreach(_ ! CloseConnection)
      }
  }
}

object UnitRunActor {

  case object InitUnitRun

  case object DbConnectionInitialized

  case object StartUnitRun

  case class DbCallFinished(dbResult: DbResult)

  case object DbConnectionTerminated
}
