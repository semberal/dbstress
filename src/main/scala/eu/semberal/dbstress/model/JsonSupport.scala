package eu.semberal.dbstress.model

import com.typesafe.scalalogging.slf4j.LazyLogging
import eu.semberal.dbstress.Defaults._
import eu.semberal.dbstress.model.Results._
import org.joda.time.Duration
import play.api.libs.functional.syntax._
import play.api.libs.json._

object JsonSupport extends LazyLogging {

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

  implicit val unitSummaryWrites: Writes[UnitSummary] =
    ((__ \ "expectedDbCalls").write[Int] ~
      (__ \ "executedDbCalls").write[Int] ~
      (__ \ "notExecutedDbCalls").write[Int] ~
      (__ \ "successfulDbCalls").write[Int] ~
      (__ \ "failedDbCalls").write[Int]) apply { s => (s.expectedDbCalls, s.executedDbCalls, s.notExecutedDbCalls, s.successfulDbCalls, s.failedDbCalls)
    }

  implicit val unitResultWrites: Writes[UnitResult] =
    ((__ \ "name").write[String] ~
      (__ \ "unitSummary").write[UnitSummary] ~
      (__ \ "unitRuns").write[List[UnitRunResult]]) apply { r =>
      (r.unitConfig.name, r.summary, r.unitRunResults)
    }

  private def toJsObject(stmtResult: StatementResult): JsObject = stmtResult match {
    case FetchedRows(n) => JsObject(Seq("fetchedRows" -> JsNumber(n)))
    case UpdateCount(n) => JsObject(Seq("updateCount" -> JsNumber(n)))
  }
}
