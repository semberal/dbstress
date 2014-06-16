package eu.semberal.dbstress.actor

import akka.actor.{Actor, ActorLogging, Props}
import eu.semberal.dbstress.model.{Scenario, UnitResult}

class ManagerActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case Scenario(units) =>
      resultWaitReceive(units.length, Nil).foreach(context.become)
      units.foreach(u => {
        context.actorOf(Props[UnitActor], u.name) ! u
      })
  }

  def resultWaitReceive(waiting: Int, unitResults: List[UnitResult]): Option[Receive] =
    if (waiting == 0) {
      context.system.shutdown()
      None
    } else {
      val pf: PartialFunction[Any, Unit] = {
        case x: UnitResult =>
          log.info(s"action=unit_result_received success=${x.percentSuccess}% failure=${x.percentFailure}% duration_avg=${x.avgDuration}ms")
          resultWaitReceive(waiting - 1, x :: unitResults).foreach(context.become)
      }
      Some(pf)
    }
}
