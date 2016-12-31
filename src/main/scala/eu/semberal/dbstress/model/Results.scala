package eu.semberal.dbstress.model

import eu.semberal.dbstress.model.Configuration.UnitConfig

object Results {

  trait OperationResult {
    val duration: Long
  }

  case class DbConnInitResult(duration: Long) extends OperationResult

  sealed trait DbCallResult extends OperationResult {
    val dbCallId: DbCallId
  }

  case class DbCallId(scenarioId: String, connectionId: String, statementId: String) {
    override def toString = s"${scenarioId}_${connectionId}_$statementId"
  }

  case class DbCallSuccess(duration: Long, dbCallId: DbCallId, stmtResult: StatementResult) extends DbCallResult

  case class DbCallFailure(duration: Long, dbCallId: DbCallId, e: Throwable) extends DbCallResult

  sealed trait StatementResult

  case class FetchedRows(n: Int) extends StatementResult

  case class UpdateCount(n: Int) extends StatementResult

  case class UnitRunResult(connInitResult: DbConnInitResult, callResults: List[DbCallResult])

  case class UnitResult(unitConfig: UnitConfig, unitRunResults: List[UnitRunResult]) {
    lazy val summary: UnitSummary = {

      val allDbCalls = unitRunResults.flatMap(_.callResults)
      val successfulDbCalls = allDbCalls.collect({ case e: DbCallSuccess => e })
      val failedDbCalls = allDbCalls.collect({ case e: DbCallFailure => e })

      val connInits = unitRunResults.map(_.connInitResult)

      UnitSummary(
        StatsResults(connInits.map(_.duration)),

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
    connectionInitsSummary: StatsResults,

    expectedDbCalls: Int,

    executedDbCallsSummary: StatsResults,
    successfulDbCallsSummary: StatsResults,
    failedDbCallsSummary: StatsResults)

  class ConnectionInitException(e: Throwable) extends RuntimeException(e)

  class UnitRunException(e: Throwable) extends RuntimeException(e)

}
