package eu.semberal.dbstress.model

import eu.semberal.dbstress.model.Configuration.UnitConfig
import org.joda.time.{DateTime, Duration}

object Results {

  trait OperationResult {
    val start: DateTime

    val finish: DateTime

    lazy val duration = new Duration(start, finish).getMillis
  }

  sealed trait DbConnInitResult extends OperationResult

  case class DbConnInitSuccess(start: DateTime, finish: DateTime) extends DbConnInitResult

  case class DbConnInitFailure(start: DateTime, finish: DateTime, e: Throwable) extends DbConnInitResult

  sealed trait DbCallResult extends OperationResult {
    val dbCallId: DbCallId
  }

  case class DbCallId(scenarioId: String, connectionId: String, statementId: String) {
    override def toString = s"${scenarioId}_${connectionId}_$statementId"
  }

  case class DbCallSuccess(start: DateTime, finish: DateTime, dbCallId: DbCallId, stmtResult: StatementResult) extends DbCallResult

  case class DbCallFailure(start: DateTime, finish: DateTime, dbCallId: DbCallId, e: Throwable) extends DbCallResult

  sealed trait StatementResult

  case class FetchedRows(n: Int) extends StatementResult

  case class UpdateCount(n: Int) extends StatementResult

  case class UnitRunResult(connInitResult: DbConnInitResult, callResults: List[DbCallResult])

  case class UnitResult(unitConfig: UnitConfig, unitRunResults: List[UnitRunResult]) {
    lazy val summary = {

      val allDbCalls = unitRunResults.flatMap(_.callResults)
      val successfulDbCalls = allDbCalls.collect({ case e: DbCallSuccess => e})
      val failedDbCalls = allDbCalls.collect({ case e: DbCallFailure => e})

      val allConnInits = unitRunResults.map(_.connInitResult)
      val successfulConnInits = allConnInits.collect({ case e: DbConnInitSuccess => e})
      val failedConnInits = allConnInits.collect({ case e: DbConnInitFailure => e})

      UnitSummary(
        StatsResults(allConnInits.map(_.duration)),
        StatsResults(successfulConnInits.map(_.duration)),
        StatsResults(failedConnInits.map(_.duration)),

        unitConfig.parallelConnections * unitConfig.config.repeats,

        StatsResults(allDbCalls.map(_.duration)),
        StatsResults(successfulDbCalls.map(_.duration)),
        StatsResults(failedDbCalls.map(_.duration))
      )
    }
  }

  case class ScenarioResult(unitResults: List[UnitResult])

  case class UnitSummary
  (
    performedConnectionInitsSummary: StatsResults,
    successfulConnectionInitsSummary: StatsResults,
    failedConnectionInitsSummary: StatsResults,

    expectedDbCalls: Int,

    executedDbCallsSummary: StatsResults,
    successfulDbCallsSummary: StatsResults,
    failedDbCallsSummary: StatsResults)

}
