package eu.semberal.dbstress.actor

import akka.actor.Actor
import akka.event.LoggingReceive
import com.typesafe.scalalogging.slf4j.LazyLogging
import eu.semberal.dbstress.actor.TerminatorActor.ScenarioCompleted

class TerminatorActor extends Actor with LazyLogging {

  override def receive: Receive = LoggingReceive {
    case ScenarioCompleted =>
      context.become(noop)
      context.system.shutdown()
  }

  val noop: Receive = LoggingReceive {
    case ScenarioCompleted =>
      logger.warn("Terminate message received, system has already been shut down")
  }
}

case object TerminatorActor {

  case object ScenarioCompleted

}
