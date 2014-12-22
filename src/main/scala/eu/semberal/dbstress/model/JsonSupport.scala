package eu.semberal.dbstress.model

import eu.semberal.dbstress.Defaults._
import eu.semberal.dbstress.model.Configuration.{DbCommunicationConfigWithoutPassword, DbCommunicationConfig,
UnitConfig, UnitRunConfig}
import eu.semberal.dbstress.model.Results._
import eu.semberal.dbstress.util.ModelExtensions._
import org.joda.time.Duration
import play.api.libs.functional.syntax._
import play.api.libs.json._

object JsonSupport {

  implicit val dbCallIdWrites: Writes[DbCallId] = new Writes[DbCallId] {
    override def writes(o: DbCallId): JsValue = JsString(o.toString)
  }

  implicit val dbCallResultWrites: Writes[DbCallResult] =
    ((__ \ "successful").write[Boolean] ~
      (__ \ "errorMessage").writeNullable[String] ~
      (__ \ "statementResult").writeNullable[JsObject] ~
      (__ \ "callStart").write[String] ~
      (__ \ "callEnd").write[String] ~
      (__ \ "duration").write[Long] ~
      (__ \ "callId").write[DbCallId]) apply (_ match {
      case DbCallSuccess(start, finish, dbCallId, stmtResult) =>
        (true, None, Some(toJsObject(stmtResult)), dateTimeFormat.print(start), dateTimeFormat.print(finish), new Duration(start, finish).getMillis, dbCallId)
      case DbCallFailure(start, finish, dbCallId, e) =>
        (false, Some(e.toString), None, dateTimeFormat.print(start), dateTimeFormat.print(finish), new Duration(start, finish).getMillis, dbCallId)
    })

  implicit val dbConnectionInitResultWrites: Writes[DbConnInitResult] =
    ((__ \ "successful").write[Boolean] ~
      (__ \ "errorMessage").writeNullable[String] ~
      (__ \ "initStart").write[String] ~
      (__ \ "initEnd").write[String] ~
      (__ \ "duration").write[Long]) apply (_ match {
      case DbConnInitSuccess(start, finish) =>
        (true, None, dateTimeFormat.print(start), dateTimeFormat.print(finish), new Duration(start, finish).getMillis)
      case DbConnInitFailure(start, finish, e) =>
        (false, Some(e.toString), dateTimeFormat.print(start), dateTimeFormat.print(finish), new Duration(start, finish).getMillis)
    })

  implicit val unitRunResultWrites: Writes[UnitRunResult] =
    ((__ \ "connectionInit").write[DbConnInitResult] ~
      (__ \ "callResults").write[Seq[DbCallResult]]) apply unlift(UnitRunResult.unapply)


  implicit val statsWrites: Writes[StatsResults] = new Writes[StatsResults] {
    override def writes(o: StatsResults): JsValue =
      JsObject(Seq(
        "count" -> JsNumber(o.count),
        "min" -> o.min.getJsNumber,
        "max" -> o.max.getJsNumber,
        "mean" -> o.mean.getJsNumber,
        "median" -> o.median.getJsNumber,
        "stddev" -> o.stddev.getJsNumber
      ))
  }

  implicit val unitSummaryWrites: Writes[UnitSummary] =
    ((__ \ "connInits" \ "executed").write[StatsResults] ~
      (__ \ "connInits" \ "executed" \ "successful").write[StatsResults] ~
      (__ \ "connInits" \ "executed" \ "failed").write[StatsResults] ~

      (__ \ "expectedDbCalls").write[Int] ~

      (__ \ "dbCalls" \ "executed").write[StatsResults] ~
      (__ \ "dbCalls" \ "executed" \ "successful").write[StatsResults] ~
      (__ \ "dbCalls" \ "executed" \ "failed").write[StatsResults]
      ) apply unlift(UnitSummary.unapply)

  implicit val dbCommunicationConfigWrites: Writes[DbCommunicationConfig] =
    ((__ \ "uri").write[String] ~
      (__ \ "driverClass").writeNullable[String] ~
      (__ \ "username").write[String] ~
      (__ \ "query").write[String] ~
      (__ \ "connectionTimeout").writeNullable[Int] ~
      (__ \ "queryTimeout").writeNullable[Int]) apply unlift(DbCommunicationConfigWithoutPassword.unapply)

  implicit val unitRunConfigWrites: Writes[UnitRunConfig] =
    ((__ \ "databaseConfig").write[DbCommunicationConfig] ~
      (__ \ "repeats").write[Int]) apply unlift(UnitRunConfig.unapply)

  implicit val unitConfigWrites: Writes[UnitConfig] =
    ((__ \ "name").write[String] ~
      (__ \ "description").writeNullable[String] ~
      (__ \ "unitRunConfig").write[UnitRunConfig] ~
      (__ \ "parallelConnections").write[Int]) apply unlift(UnitConfig.unapply)

  implicit val unitResultWrites: Writes[UnitResult] =
    ((__ \ "configuration").write[UnitConfig] ~
      (__ \ "unitSummary").write[UnitSummary] ~
      (__ \ "unitRuns").write[List[UnitRunResult]]) apply (r => (r.unitConfig, r.summary, r.unitRunResults))

  implicit val scenarioResultWrites: Writes[ScenarioResult] = new Writes[ScenarioResult] {
    override def writes(o: ScenarioResult): JsValue = JsObject(Seq(
      "scenarioResult" -> JsObject(Seq(
        "unitResults" -> Json.toJson(o.unitResults)
      ))
    ))
  }

  private def toJsObject(stmtResult: StatementResult): JsObject = stmtResult match {
    case FetchedRows(n) => JsObject(Seq("fetchedRows" -> JsNumber(n)))
    case UpdateCount(n) => JsObject(Seq("updateCount" -> JsNumber(n)))
  }
}
