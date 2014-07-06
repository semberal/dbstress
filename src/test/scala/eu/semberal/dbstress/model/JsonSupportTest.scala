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
    val model = DbCallSuccess(start, end, FetchedRows(77))
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
    val model = UnitRunResult(DbConnInitSuccess(start, end), List(DbCallSuccess(start, end, UpdateCount(12)),
      DbCallSuccess(start, end, FetchedRows(77)), DbCallFailure(start, end, ex)))
    toJson(model) should be(expectedUnitRunResult)
  }

  it should "correctly serialize the UnitResult model object" in {
    toJson(unitResult)
  }

  ignore should "correctly serialize the UnitSummary model object" in { // todo fix test
    val expected = JsObject(Seq(
      "expectedDbCalls" -> JsNumber(55),
      "executedDbCalls" -> JsNumber(2),
      "notExecutedDbCalls" -> JsNumber(53),
      "successfulDbCalls" -> JsNumber(1),
      "failedDbCalls" -> JsNumber(1)
    ))
    toJson(unitResult.summary) should be(expected)
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

  val unitResult = {
    val dbCommunicationConfig = DbCommunicationConfig("A", "B", "C", "D", "E", 10, 10)
    val unitRunResults = UnitRunResult(DbConnInitSuccess(start, end), List(
      DbCallSuccess(start, end, FetchedRows(10)),
      DbCallFailure(start, end, new RuntimeException)
    ))
    UnitResult(UnitConfig("unit1", "This is unit1", UnitRunConfig(dbCommunicationConfig, repeats), parallel), List(unitRunResults))
  }

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
        "fetchedRows" -> JsNumber(77)
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
}