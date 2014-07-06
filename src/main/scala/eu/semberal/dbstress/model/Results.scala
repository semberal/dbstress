package eu.semberal.dbstress.model

import eu.semberal.dbstress.model.Configuration.UnitConfig
import org.joda.time.{DateTime, Duration}
import eu.semberal.dbstress.util.ModelExtensions._

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

      /* todo The conversion to double is ugly here. Take a loot at breeze how to handle longs */
      val durationFunction: DbCallResult => Double = x => new Duration(x.start, x.finish).getMillis.toDouble

      val expectedDbCalls = unitConfig.parallelConnections * unitConfig.config.repeats
      val executedDbCalls = flattened.length
      val notExecutedDbCalls = expectedDbCalls - executedDbCalls
      val successfulDbCalls = successes.length

      val executedDbCallsDurations = flattened map durationFunction
      val successesDurations = successes map durationFunction
      val failuresDurations = failures map durationFunction

      val failedDbCalls = failures.length
      UnitSummary(expectedDbCalls, executedDbCalls, notExecutedDbCalls, successfulDbCalls, failedDbCalls,

        executedDbCallsDurations.minimum, executedDbCallsDurations.maximum, executedDbCallsDurations.mean,
        executedDbCallsDurations.median, executedDbCallsDurations.stddev,

        successesDurations.minimum, successesDurations.maximum, successesDurations.mean,
        successesDurations.median, successesDurations.stddev,

        failuresDurations.minimum, failuresDurations.maximum, failuresDurations.mean,
        failuresDurations.median, failuresDurations.stddev
      )
    }
  }

  case class UnitSummary // todo will be necessary to split into multiple smaller case classes or use HList
  (
    expectedDbCalls: Int, executedDbCalls: Int, notExecutedDbCalls: Int, successfulDbCalls: Int, failedDbCalls: Int,

    executedDbCallsMin: Option[Double], executedDbCallsMax: Option[Double], executedDbCallsMean: Option[Double],
    executedDbCallsMedian: Option[Double], executedDbCallsStddev: Option[Double],

    successfulDbCallsMin: Option[Double], successfulDbCallsMax: Option[Double], successfulDbCallsMean: Option[Double],
    successfulDbCallsMedian: Option[Double], successfulDbCallsStddev: Option[Double],

    failedDbCallsMin: Option[Double], failedDbCallsMax: Option[Double], failedDbCallsMean: Option[Double],
    failedDbCallsMedian: Option[Double], failedDbCallsStddev: Option[Double]
    )

}
