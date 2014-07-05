package eu.semberal.dbstress.actor

import akka.actor.Actor
import com.typesafe.scalalogging.slf4j.LazyLogging
import eu.semberal.dbstress.actor.TerminatorActor.ScenarioCompleted

class TerminatorActor extends Actor with LazyLogging {

  override def receive: Receive = {
    case ScenarioCompleted =>
      logger.debug("Shutting down the actor system")
      context.become(noop)
      context.system.shutdown()
  }

  val noop: Receive = {
    case ScenarioCompleted =>
      logger.warn("Terminate message received, system has already been shut down")
  }
}

case object TerminatorActor {

  case object ScenarioCompleted

}
