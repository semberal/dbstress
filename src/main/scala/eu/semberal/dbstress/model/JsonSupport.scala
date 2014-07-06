package eu.semberal.dbstress.model

import com.typesafe.scalalogging.slf4j.LazyLogging
import eu.semberal.dbstress.Defaults._
import eu.semberal.dbstress.model.Configuration.{DbCommunicationConfig, UnitConfig, UnitRunConfig}
import eu.semberal.dbstress.model.Results._
import org.joda.time.Duration
import play.api.libs.functional.syntax._
import play.api.libs.json._

object JsonSupport extends LazyLogging {

  implicit class NullableJsonValue(value: Option[Double]) {
    def getJsNumber: JsValue = value.map(JsNumber(_)).getOrElse(JsNull)
  }

  implicit val dbCallResultWrites: Writes[DbCallResult] =
    ((__ \ "successful").write[Boolean] ~
      (__ \ "errorMessage").writeNullable[String] ~
      (__ \ "statementResult").writeNullable[JsObject] ~
      (__ \ "callStart").write[String] ~
      (__ \ "callEnd").write[String] ~
      (__ \ "duration").write[Long]) apply (_ match {
      case DbCallSuccess(start, finish, stmtResult) =>
        (true, None, Some(toJsObject(stmtResult)), dateTimeFormat.print(start), dateTimeFormat.print(finish), new Duration(start, finish).getMillis)
      case DbCallFailure(start, finish, e) =>
        (false, Some(e.toString), None, dateTimeFormat.print(start), dateTimeFormat.print(finish), new Duration(start, finish).getMillis)
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


  implicit val foo: Writes[UnitSummary] = new Writes[UnitSummary] {

    override def writes(o: UnitSummary): JsValue =

      JsObject(Seq(
        "expectedDbCalls" -> JsNumber(o.expectedDbCalls),
        "executedDbCalls" -> JsNumber(o.executedDbCalls),
        "notExecutedDbCalls" -> JsNumber(o.notExecutedDbCalls),
        "successfulDbCalls" -> JsNumber(o.successfulDbCalls),
        "failedDbCalls" -> JsNumber(o.failedDbCalls),

        "executedDbCallsMin" -> o.executedDbCallsMin.getJsNumber,
        "executedDbCallsMax" -> o.executedDbCallsMax.getJsNumber,
        "executedDbCallsMean" -> o.executedDbCallsMean.getJsNumber,
        "executedDbCallsMedian" -> o.executedDbCallsMedian.getJsNumber,
        "executedDbCallsStddev" -> o.executedDbCallsStddev.getJsNumber,

        "successfulDbCallsMin" -> o.successfulDbCallsMin.getJsNumber,
        "successfulDbCallsMax" -> o.successfulDbCallsMax.getJsNumber,
        "successfulDbCallsMean" -> o.successfulDbCallsMean.getJsNumber,
        "successfulDbCallsMedian" -> o.successfulDbCallsMedian.getJsNumber,
        "successfulDbCallsStddev" -> o.successfulDbCallsStddev.getJsNumber,

        "failedDbCallsMin" -> o.failedDbCallsMin.getJsNumber,
        "failedDbCallsMax" -> o.failedDbCallsMax.getJsNumber,
        "failedDbCallsMean" -> o.failedDbCallsMean.getJsNumber,
        "failedDbCallsMedian" -> o.failedDbCallsMedian.getJsNumber,
        "failedDbCallsStddev" -> o.failedDbCallsStddev.getJsNumber
      ))
  }

  implicit val dbCommunicationConfigWrites: Writes[DbCommunicationConfig] =
    ((__ \ "uri").write[String] ~
      (__ \ "driverClass").write[String] ~
      (__ \ "username").write[String] ~
      (__ \ "password").write[String] ~
      (__ \ "query").write[String] ~
      (__ \ "connectionTimeout").write[Int] ~
      (__ \ "queryTimeout").write[Int]) apply unlift(DbCommunicationConfig.unapply)

  implicit val unitRunConfigWrites: Writes[UnitRunConfig] =
    ((__ \ "databaseConfig").write[DbCommunicationConfig] ~
      (__ \ "repeats").write[Int]) apply unlift(UnitRunConfig.unapply)

  implicit val unitConfigWrites: Writes[UnitConfig] =
    ((__ \ "name").write[String] ~
      (__ \ "description").write[String] ~
      (__ \ "unitRunConfig").write[UnitRunConfig] ~
      (__ \ "parallelConnections").write[Int]) apply unlift(UnitConfig.unapply)

  implicit val unitResultWrites: Writes[UnitResult] =
    ((__ \ "configuation").write[UnitConfig] ~
      (__ \ "unitSummary").write[UnitSummary] ~
      (__ \ "unitRuns").write[List[UnitRunResult]]) apply (r => (r.unitConfig, r.summary, r.unitRunResults))

  private def toJsObject(stmtResult: StatementResult): JsObject = stmtResult match {
    case FetchedRows(n) => JsObject(Seq("fetchedRows" -> JsNumber(n)))
    case UpdateCount(n) => JsObject(Seq("updateCount" -> JsNumber(n)))
  }
}
