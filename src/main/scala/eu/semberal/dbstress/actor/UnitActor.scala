package eu.semberal.dbstress.actor

import java.util.concurrent.TimeUnit.SECONDS

import akka.actor.{Actor, ActorLogging, Props}
import akka.event.LoggingReceive
import akka.pattern.ask
import akka.util.Timeout
import eu.semberal.dbstress.model.{TestUnit, UnitRunResult}

class UnitActor extends Actor with ActorLogging {

  override def receive: Receive = LoggingReceive {
    case TestUnit(config, parallelUsers) =>
      implicit val timeout = Timeout(2, SECONDS)
      (1 to parallelUsers).map(i => {
        val f = context.actorOf(Props[UnitRunActor]).ask(config).mapTo[UnitRunResult]
        val s = sender()
        f.


      })
    case UnitRunResult(success, start, finish, results) =>

  }
}

