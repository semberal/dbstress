package eu.semberal.dbstress.actor

import java.sql.SQLException

import akka.actor.SupervisorStrategy.{Decider, Escalate, defaultDecider}
import akka.actor._
import eu.semberal.dbstress.actor.DbCommActor.{Init, NextRound}
import eu.semberal.dbstress.model._

class UnitRunActor extends Actor with ActorLogging {

  private val CommunicationActorName = "dbCommunicationActor"

  private var remainingRepeats: Int = _

  private var dbConfig: DbConfig = _

  override def receive: Receive = configWait

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    val d: Decider = {
      case e: SQLException =>
        context.stop(self)
        Escalate
    }
    d orElse defaultDecider
  }

  def resultWait: Receive = {
    case dbResult: DbResult =>
      context.parent ! dbResult
      remainingRepeats -= 1

      if (remainingRepeats > 0) {
        context.child(CommunicationActorName).foreach(_ ! NextRound)
      }
  }

  val configWait: Receive = {
    case TestUnitConfig(config, repeats) =>
      this.dbConfig = config
      context.become(resultWait) // todo become for next message?
      remainingRepeats = repeats
      val dbWorker = context.actorOf(Props(classOf[DbCommActor], config), CommunicationActorName)
      dbWorker ! Init
      dbWorker ! NextRound
  }
}
