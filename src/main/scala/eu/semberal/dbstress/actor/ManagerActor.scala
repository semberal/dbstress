package eu.semberal.dbstress.actor

import akka.actor.{ActorLogging, Props, Actor}
import akka.event.LoggingReceive
import eu.semberal.dbstress.model.Scenario

class ManagerActor extends Actor with ActorLogging {

  override def receive: Receive = LoggingReceive {
    case Scenario(units, repeats) =>
      units.foreach(u => {
        context.actorOf(Props[UnitActor]) ! u
      })
  }
}
