package eu.semberal.dbstress.actor

import java.io.{BufferedWriter, FileWriter}

import akka.actor.{Actor, ActorLogging, Props}
import eu.semberal.dbstress.model.{Scenario, UnitResult}
import org.duh.resource._

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
      // todo some proper csv library
      for (b <- new BufferedWriter(new FileWriter("/home/semberal/Desktop/result.csv")).auto) {
        val header = List("name", "total_queries_count",
          "succ_queries_count", "succ_percent", "succ_avg_duration",
          "fail_queries_count", "fail_percent", "fail_avg_duration")
        b.write(header.mkString(","))
        b.newLine()
        unitResults foreach { result =>
          val s = List(result.name, result.dbResults.size,
            result.successes.length, result.percentSuccess, result.avgSuccessDuration.getOrElse("-"),
            result.failures.length, result.percentFailure, result.avgFailedDuration.getOrElse("-")).mkString(",")
          b.write(s)
          b.newLine()
        }
      }
      None
    } else {
      val pf: PartialFunction[Any, Unit] = {
        case x: UnitResult =>
          resultWaitReceive(waiting - 1, x :: unitResults).foreach(context.become)
      }
      Some(pf)
    }
}
