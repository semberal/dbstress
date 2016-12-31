package eu.semberal.dbstress.model

import eu.semberal.dbstress.model.Configuration.{DbCommunicationConfig, UnitConfig, UnitRunConfig}
import eu.semberal.dbstress.model.Results._
import eu.semberal.dbstress.model.UnitSummaryTest.unitResult
import org.scalatest.{FlatSpec, Matchers}

class UnitSummaryTest extends FlatSpec with Matchers {

  private val summary = unitResult.summary

  behavior of "UnitSummary"

  it should "correctly calculate summary information" in {
    summary.expectedDbCalls should be(10)
    summary.executedDbCallsSummary.count should be(2)
    summary.successfulDbCallsSummary.count should be(1)
    summary.failedDbCallsSummary.count should be(1)
  }
}

object UnitSummaryTest {
  private val repeats = 5
  private val parallel = 2
  private val unitResult = {
    val dbCommunicationConfig = DbCommunicationConfig("A", Some("B"), "C", "D", "E", Some(10))
    val unitRunResults = UnitRunResult(DbConnInitResult(1), List(
      DbCallSuccess(1, DbCallId("1", "2", "3"), FetchedRows(10)),
      DbCallFailure(2, DbCallId("4", "5", "6"), new RuntimeException)
    ))
    UnitResult(UnitConfig("unit1", Some("This is unit1"), UnitRunConfig(dbCommunicationConfig, repeats), parallel), List(unitRunResults))
  }
}
