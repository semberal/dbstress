package eu.semberal.dbstress.model

import eu.semberal.dbstress.model.Configuration.{DbCommunicationConfig, UnitConfig, UnitRunConfig}
import eu.semberal.dbstress.model.JsonSupport._
import eu.semberal.dbstress.model.JsonSupportTest._
import eu.semberal.dbstress.model.Results._
import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.Json.toJson
import play.api.libs.json._

class JsonSupportTest extends FlatSpec with Matchers {

  behavior of "DbCallResult json writer"

  it should "correctly serialize the DbCallSuccess model object with update count" in {
    val model = DbCallSuccess(start, end, UpdateCount(12))
    toJson(model) should be(expectedDbCallSuccessWithUpdateCount)
  }

  it should "correctly serialize the DbCallSuccess model object with fetched rows" in {
    val model = DbCallSuccess(start, end, FetchedRows(10))
    toJson(model) should be(expectedDbCallSuccessWithFetchedRows)
  }

  it should "correctly serialize the DbCallFailure model object" in {
    val model = DbCallFailure(start, end, ex)
    toJson(model) should be(expectedDbCallFailure)
  }

  it should "correctly serialize the DbConnInitSuccess model object" in {
    val model = DbConnInitSuccess(start, end)
    toJson(model) should be(expectedDbConnInitSuccess)
  }

  it should "correctly serialize the DbConnInitFailure model object" in {
    val model = DbConnInitFailure(start, end, ex)
    val expected = JsObject(Seq(
      "duration" -> JsNumber(duration),
      "initStart" -> JsString(startStr),
      "successful" -> JsBoolean(false),
      "initEnd" -> JsString(endStr),
      "errorMessage" -> JsString(ex.toString)
    ))
    toJson(model) should be(expected)
  }

  it should "correctly serialize the UnitRunResult model object with empty DbCallResults" in {
    val model = UnitRunResult(DbConnInitSuccess(start, end), Nil)

    val expected = JsObject(Seq(
      "connectionInit" -> JsObject(Seq(
        "successful" -> JsBoolean(true),
        "initStart" -> JsString(startStr),
        "initEnd" -> JsString(endStr),
        "duration" -> JsNumber(duration)
      )),
      "callResults" -> JsArray(Nil)
    ))

    toJson(model) should be(expected)
  }

  it should "correctly serialize the UnitRunResult model object with non-empty DbCallResults" in {

    toJson(unitRunResult) should be(expectedUnitRunResult)
  }

  it should "correctly serialize the UnitResult model object" in {
    toJson(unitResult) should be(expectedUnitResult)
  }

  it should "correctly serialize UnitConfiguration model object" in {
    toJson(unitConfig) should be(expectedUnitConfig)
  }

  it should "correctly serialize the UnitSummary model object" in {
    toJson(unitResult.summary) should be(expectedUnitSummary)
  }
}

object JsonSupportTest {

  val ex = new RuntimeException("There was an error!")

  val duration = 1044
  val start = new DateTime(2010, 5, 18, 15, 32, 33, 687, DateTimeZone.forID("Europe/Prague"))
  val end = start.plusMillis(duration)
  val startStr = "2010-05-18T15:32:33.687+02:00"
  val endStr = "2010-05-18T15:32:34.731+02:00"
  val repeats = 11
  val parallel = 5
  val unitName = "unit1"
  val unitDescription = "This is unit1"

  val unitRunResult = UnitRunResult(DbConnInitSuccess(start, end), List(DbCallSuccess(start, end, UpdateCount(12)),
    DbCallSuccess(start, end, FetchedRows(10)), DbCallFailure(start, end, ex)))

  val unitConfig = UnitConfig(unitName, Some(unitDescription), UnitRunConfig(DbCommunicationConfig("A", Some("B"), "C", "D", "E", Some(10), Some(15)), repeats), parallel)

  val unitResult = UnitResult(unitConfig, List(unitRunResult))

  val expectedDbConnInitSuccess = JsObject(
    Seq(
      "duration" -> JsNumber(duration),
      "initStart" -> JsString(startStr),
      "successful" -> JsBoolean(true),
      "initEnd" -> JsString(endStr)
    )
  )

  val expectedDbCallSuccessWithUpdateCount = JsObject(
    Seq(
      "callStart" -> JsString(startStr),
      "successful" -> JsBoolean(true),
      "callEnd" -> JsString(endStr),
      "duration" -> JsNumber(duration),
      "statementResult" -> JsObject(Seq(
        "updateCount" -> JsNumber(12)
      ))
    )
  )

  val expectedDbCallSuccessWithFetchedRows = JsObject(
    Seq(
      "callStart" -> JsString(startStr),
      "successful" -> JsBoolean(true),
      "callEnd" -> JsString(endStr),
      "duration" -> JsNumber(duration),
      "statementResult" -> JsObject(Seq(
        "fetchedRows" -> JsNumber(10)
      ))
    )
  )

  val expectedDbCallFailure = JsObject(
    Seq(
      "errorMessage" -> JsString(ex.toString),
      "duration" -> JsNumber(duration),
      "callStart" -> JsString(startStr),
      "successful" -> JsBoolean(false),
      "callEnd" -> JsString(endStr)
    )
  )

  val expectedUnitRunResult = JsObject(Seq(
    "connectionInit" -> expectedDbConnInitSuccess,
    "callResults" -> JsArray(List(
      expectedDbCallSuccessWithUpdateCount,
      expectedDbCallSuccessWithFetchedRows,
      expectedDbCallFailure
    ))
  ))

  val expectedUnitSummary = JsObject(Seq(
    "connInits" -> JsObject(Seq(
      "executed" -> JsObject(Seq(
        "count" -> JsNumber(1),
        "min" -> JsNumber(duration),
        "max" -> JsNumber(duration),
        "mean" -> JsNumber(duration),
        "median" -> JsNumber(duration),
        "stddev" -> JsNumber(0.0),
        "successful" -> JsObject(Seq(
          "count" -> JsNumber(1),
          "min" -> JsNumber(duration),
          "max" -> JsNumber(duration),
          "mean" -> JsNumber(duration),
          "median" -> JsNumber(duration),
          "stddev" -> JsNumber(0.0)
        )),
        "failed" -> JsObject(Seq(
          "count" -> JsNumber(0),
          "min" -> JsNull,
          "max" -> JsNull,
          "mean" -> JsNull,
          "median" -> JsNull,
          "stddev" -> JsNull
        ))
      ))
    )),
    "expectedDbCalls" -> JsNumber(repeats * parallel),
    "dbCalls" -> JsObject(Seq(
      "executed" -> JsObject(Seq(
        "count" -> JsNumber(3),
        "min" -> JsNumber(duration),
        "max" -> JsNumber(duration),
        "mean" -> JsNumber(duration),
        "median" -> JsNumber(duration),
        "stddev" -> JsNumber(0),
        "successful" -> JsObject(Seq(
          "count" -> JsNumber(2),
          "min" -> JsNumber(duration),
          "max" -> JsNumber(duration),
          "mean" -> JsNumber(duration),
          "median" -> JsNumber(duration),
          "stddev" -> JsNumber(0.0)
        )),
        "failed" -> JsObject(Seq(
          "count" -> JsNumber(1),
          "min" -> JsNumber(duration),
          "max" -> JsNumber(duration),
          "mean" -> JsNumber(duration),
          "median" -> JsNumber(duration),
          "stddev" -> JsNumber(0)
        ))
      ))
    ))
  ))

  val expectedUnitConfig = JsObject(Seq(
    "name" -> JsString(unitName),
    "description" -> JsString(unitDescription),
    "parallelConnections" -> JsNumber(parallel),
    "unitRunConfig" -> JsObject(Seq(
      "repeats" -> JsNumber(repeats),
      "databaseConfig" -> JsObject(Seq(
        "uri" -> JsString("A"),
        "driverClass" -> JsString("B"),
        "username" -> JsString("C"),
        "password" -> JsString("D"),
        "query" -> JsString("E"),
        "connectionTimeout" -> JsNumber(10),
        "queryTimeout" -> JsNumber(15)
      ))
    ))

  ))

  val expectedUnitResult = JsObject(Seq(
    "unitSummary" -> expectedUnitSummary,
    "unitRuns" -> JsArray(Seq(expectedUnitRunResult)),
    "configuration" -> expectedUnitConfig
  ))
}