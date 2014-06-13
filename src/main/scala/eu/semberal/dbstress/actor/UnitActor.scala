package eu.semberal.dbstress.actor

import akka.actor.Actor
import eu.semberal.dbstress.model.TestUnit

class UnitActor extends Actor {
  override def receive: Receive = {
    case TestUnit(configd, parallelUsers) =>

  }
}
