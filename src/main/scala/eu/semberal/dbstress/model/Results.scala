package eu.semberal.dbstress.model

import eu.semberal.dbstress.model.Configuration.UnitConfig
import org.joda.time.{DateTime, Duration}

object Results {

  /* Connection init results */
  sealed trait DbConnInitResult

  case class DbConnInitSuccess(start: DateTime, finish: DateTime) extends DbConnInitResult

  case class DbConnInitFailure(start: DateTime, finish: DateTime, e: Throwable) extends DbConnInitResult


  /* Query results */
  sealed trait DbCallResult {
    val start: DateTime
    val finish: DateTime
  }

  case class DbCallSuccess(start: DateTime, finish: DateTime, stmtResult: StatementResult) extends DbCallResult

  case class DbCallFailure(start: DateTime, finish: DateTime, e: Throwable) extends DbCallResult


  /* Low level DB statement results */
  sealed trait StatementResult

  case class FetchedRows(n: Int) extends StatementResult

  case class UpdateCount(n: Int) extends StatementResult

  /* Unit run result*/
  case class UnitRunResult(connInitResult: DbConnInitResult, callResults: List[DbCallResult])


  /* Unit result */
  case class UnitResult(unitConfig: UnitConfig, unitRunResults: List[UnitRunResult]) {
    lazy val summary = {
      val flattened: List[DbCallResult] = unitRunResults.flatMap(_.callResults)
      val successes = flattened.collect({ case e: DbCallSuccess => e})
      val failures = flattened.collect({ case e: DbCallFailure => e})

      val expectedDbCalls = unitConfig.parallelConnections * unitConfig.config.repeats
      val executedDbCalls = flattened.length
      val notExecutedDbCalls = expectedDbCalls - executedDbCalls
      val successfulDbCalls = successes.length
      val failedDbCalls = failures.length
      UnitSummary(expectedDbCalls, executedDbCalls, notExecutedDbCalls, successfulDbCalls, failedDbCalls)
    }
  }

  case class UnitSummary(expectedDbCalls: Int, executedDbCalls: Int, notExecutedDbCalls: Int,
                         successfulDbCalls: Int, failedDbCalls: Int)

}
