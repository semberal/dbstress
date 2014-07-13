package eu.semberal.dbstress.model

import eu.semberal.dbstress.model.Configuration.{DbCommunicationConfig, UnitConfig, UnitRunConfig}
import eu.semberal.dbstress.model.Results._
import eu.semberal.dbstress.model.UnitSummaryTest.unitResult
import org.joda.time.DateTime.now
import org.scalatest.{FlatSpec, Matchers}

class UnitSummaryTest extends FlatSpec with Matchers {

  val summary = unitResult.summary

  behavior of "UnitSummary"

  it should "correctly calculate summary information" in {
    summary.expectedDbCalls should be(10)
    summary.executedDbCallsSummary.count should be(2)
    summary.successfulSbCallsSummary.count should be(1)
    summary.failedDbCallsSummary.count should be(1)
  }
}

object UnitSummaryTest {
  val repeats = 5
  val parallel = 2
  val unitResult = {
    val dbCommunicationConfig = DbCommunicationConfig("A", "B", "C", "D", "E", 10, 10)
    val unitRunResults = UnitRunResult(DbConnInitSuccess(now(), now()), List(
      DbCallSuccess(now(), now(), FetchedRows(10)),
      DbCallFailure(now(), now(), new RuntimeException)
    ))
    UnitResult(UnitConfig("unit1", "This is unit1", UnitRunConfig(dbCommunicationConfig, repeats), parallel), List(unitRunResults))
  }
}