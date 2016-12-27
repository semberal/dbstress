package eu.semberal.dbstress.actor

import java.sql.{Connection, DriverManager}
import java.time.LocalDateTime

import akka.typed.ScalaDSL._
import akka.typed.{ActorContext, ActorRef, Behavior, DispatcherFromConfig}
import eu.semberal.dbstress.actor.ControllerBehavior._
import eu.semberal.dbstress.actor.DatabaseBehavior._
import eu.semberal.dbstress.model.Configuration.UnitRunConfig
import eu.semberal.dbstress.model.Results._
import eu.semberal.dbstress.util.IdGen
import resource.managed

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

class DatabaseBehavior(scenarioId: String, unitName: String, urConfig: UnitRunConfig) {

  private def locateDispatcher(ctx: ActorContext[_], key: String): ExecutionContextExecutor =
    ctx.system.dispatchers.lookup(DispatcherFromConfig(s"akka.dispatchers.$key"))

  private val behaviorInit: Behavior[DatabaseMsg] = ContextAware { ctx => behavior(ctx) }

  private[this] def behavior(ctx: ActorContext[DatabaseMsg]): Behavior[DatabaseMsg] = Partial {
    case InitConnection(replyTo) =>
      val dbDispatcher = locateDispatcher(ctx, "db-dispatcher")
      val systemDispatcher = ctx.system.executionContext


      val start = LocalDateTime.now()

      val future = {
        val initFuture = Future {
          urConfig.dbConfig.driverClass.foreach(Class.forName)
          DriverManager.getConnection(urConfig.dbConfig.uri, urConfig.dbConfig.username,
            urConfig.dbConfig.password)
        }(dbDispatcher)

        urConfig.dbConfig.connectionTimeout.map({ timeout =>
          val timeoutFuture = akka.pattern.after(timeout.milliseconds, ctx.system.scheduler) {
            Future.failed(new ConnectionTimeoutException(timeout))
          }(systemDispatcher)
          Future.firstCompletedOf(List(initFuture, timeoutFuture))(systemDispatcher)
        }).getOrElse(initFuture)
      }

      future.onComplete {
        case Success(c) =>
          ctx.self ! ConnectionInitSuccess(replyTo, c, DbConnInitResult(start, LocalDateTime.now()))
        case Failure(e) =>
          ctx.self ! ConnectionInitFailure(replyTo, e)
      }(systemDispatcher)
      Same

    case ConnectionInitSuccess(replyTo, c, connectionInitResult) =>
      replyTo ! UnitRunInitializationFinished
      behavior2(ctx, c, connectionInitResult)

    case ConnectionInitFailure(replyTo, e) =>
      replyTo ! UnitRunInitializationFailed(new ConnectionInitException(e))
      Stopped
  }


  private[this] def behavior2(ctx: ActorContext[DatabaseMsg],
                              connection: Connection,
                              initResult: DbConnInitResult): Behavior[DatabaseMsg] = Partial {
    case StartUnitRun(replyTo) =>
      val dbDispatcher = locateDispatcher(ctx, "db-dispatcher")
      val systemDispatcher = ctx.system.executionContext
      val connectionId = IdGen.genConnectionId()

      (1 to urConfig.repeats).foldRight[Future[List[DbCallResult]]](Future.successful(Nil)) { (_, prevFuture) =>
        prevFuture.flatMap { l =>
          Future {
            val dbCallId = DbCallId(scenarioId, connectionId, IdGen.genStatementId())
            val start = LocalDateTime.now()
            managed(connection.createStatement()).map(statement =>
              if (statement.execute(urConfig.dbConfig.query))
                FetchedRows(Iterator.continually(statement.getResultSet.next()).takeWhile(identity).length)
              else
                UpdateCount(statement.getUpdateCount)
            ).tried match {
              case Success(result) =>
                DbCallSuccess(start, LocalDateTime.now(), dbCallId, result) :: l
              case Failure(e) =>
                DbCallFailure(start, LocalDateTime.now(), dbCallId, e) :: l
            }
          }(dbDispatcher)
        }(systemDispatcher)
      }.onComplete {
        case Success(l) => replyTo ! UnitRunFinished(unitName, UnitRunResult(initResult, l))
        case Failure(e) => replyTo ! UnitRunError(new UnitRunException(e))
      }(systemDispatcher)
      Stopped
  }

}

object DatabaseBehavior {

  private class ConnectionTimeoutException(timeout: Int)
    extends RuntimeException(s"Database connection initialization timeout ($timeout ms)")

  private[actor] sealed trait DatabaseMsg

  private[actor] case class InitConnection(replyTo: ActorRef[ControllerMsg]) extends DatabaseMsg

  private case class ConnectionInitSuccess(replyTo: ActorRef[ControllerMsg], c: Connection,
                                           dbInitResult: DbConnInitResult) extends DatabaseMsg

  private case class ConnectionInitFailure(replyTo: ActorRef[ControllerMsg], e: Throwable) extends DatabaseMsg

  private[actor] case class StartUnitRun(replyTo: ActorRef[ControllerMsg]) extends DatabaseMsg

  def apply(scenarioId: String, unitName: String, urConfig: UnitRunConfig): Behavior[DatabaseMsg] =
    new DatabaseBehavior(scenarioId, unitName, urConfig).behaviorInit
}