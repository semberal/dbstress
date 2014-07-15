package eu.semberal.dbstress.model

import eu.semberal.dbstress.model.Configuration.UnitConfig
import org.joda.time.{DateTime, Duration}
import eu.semberal.dbstress.util.ModelExtensions._

object Results {

  sealed trait DbConnInitResult

  case class DbConnInitSuccess(start: DateTime, finish: DateTime) extends DbConnInitResult

  case class DbConnInitFailure(start: DateTime, finish: DateTime, e: Throwable) extends DbConnInitResult

  sealed trait DbCallResult {
    val start: DateTime
    val finish: DateTime
  }

  case class DbCallSuccess(start: DateTime, finish: DateTime, stmtResult: StatementResult) extends DbCallResult

  case class DbCallFailure(start: DateTime, finish: DateTime, e: Throwable) extends DbCallResult

  sealed trait StatementResult

  case class FetchedRows(n: Int) extends StatementResult

  case class UpdateCount(n: Int) extends StatementResult

  case class UnitRunResult(connInitResult: DbConnInitResult, callResults: List[DbCallResult])

  case class UnitResult(unitConfig: UnitConfig, unitRunResults: List[UnitRunResult]) {
    lazy val summary = {
      val flattened: List[DbCallResult] = unitRunResults.flatMap(_.callResults)
      val successes = flattened.collect({ case e: DbCallSuccess => e})
      val failures = flattened.collect({ case e: DbCallFailure => e})

      val durationFunction: DbCallResult => Long = x => new Duration(x.start, x.finish).getMillis

      UnitSummary(unitConfig.parallelConnections * unitConfig.config.repeats, StatsResults(flattened map durationFunction),
        StatsResults(successes map durationFunction), StatsResults(failures map durationFunction))
    }
  }

  case class ScenarioResult(unitResults: List[UnitResult])

  /* todo consider adding expected/successful/failed connections */
  case class UnitSummary(expectedDbCalls: Int, executedDbCallsSummary: StatsResults,
                         successfulDbCallsSummary: StatsResults, failedDbCallsSummary: StatsResults)

}
