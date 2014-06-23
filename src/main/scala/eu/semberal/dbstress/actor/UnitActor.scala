package eu.semberal.dbstress.actor

import java.sql.SQLException
import java.util.concurrent.TimeUnit.SECONDS

import akka.actor.SupervisorStrategy._
import akka.actor._
import akka.util.Timeout
import eu.semberal.dbstress.model.{DbResult, TestUnit, UnitResult}

class UnitActor extends Actor with ActorLogging {

  private var unitResult: UnitResult = _

  private var totalCount: Int = _

  private var unitName: String = _

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy(){
    val d: Decider = {
      case e: SQLException =>
        log.warning(s"An exception has occurred during unit execution unit_name=$unitName", e)
        context.parent ! UnitResult(unitName, Nil)
        Stop
    }
    d orElse defaultDecider
  }

  override def receive: Receive = {
    case TestUnit(name, unitConfig, parallelUsers) =>
      implicit val timeout = Timeout(10000, SECONDS)
      (1 to parallelUsers).map(i => {
        context.actorOf(Props[UnitRunActor], s"user${i}run") ! unitConfig
      })
      totalCount = parallelUsers * unitConfig.repeats
      unitResult = UnitResult(name, Nil)
      unitName = name

    case x: DbResult =>
      unitResult = unitResult.copy(dbResults = x :: unitResult.dbResults)
      val receivedResultsCount = unitResult.dbResults.length
//      log.debug(s"unit=$unitName received=$receivedResultsCount remaining=${totalCount - receivedResultsCount}")
      if (receivedResultsCount == totalCount) {
        context.parent ! unitResult
//        self ! PoisonPill
      }
  }
}
