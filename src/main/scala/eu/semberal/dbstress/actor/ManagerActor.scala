package eu.semberal.dbstress.actor

import akka.actor.Actor
import eu.semberal.dbstress.model.Scenario

class ManagerActor extends Actor {

  override def receive: Receive = {
    case Scenario(units, repeats) =>


  }
}
