package eu.semberal.dbstress.actor

import akka.actor.{Actor, ActorLogging, PoisonPill, Props}
import eu.semberal.dbstress.model._

class UnitRunActor extends Actor with ActorLogging {

  private val CommunicationActorName = "dbCommunicationActor"

  private var remainingRepeats: Int = _

  private var dbConfig: DbConfig = _

  override def receive: Receive = configWait

  def resultWait: Receive = {
    case dbResult: DbResult =>
      context.parent ! dbResult
      remainingRepeats -= 1

      if (remainingRepeats > 0) {
        context.child(CommunicationActorName).foreach(_ ! dbConfig)
      }
  }

  val configWait: Receive = {
    case TestUnitConfig(config, repeats) =>
      this.dbConfig = config
      context.become(resultWait) // todo become for next message?
      remainingRepeats = repeats
      context.actorOf(Props[DbCommActor], CommunicationActorName) ! config
  }
}
