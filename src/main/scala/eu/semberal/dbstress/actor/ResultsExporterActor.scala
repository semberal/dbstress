package eu.semberal.dbstress.actor

import java.io.{BufferedWriter, File, FileWriter}

import akka.actor.{Status, Actor}
import eu.semberal.dbstress.Defaults
import eu.semberal.dbstress.actor.ManagerActor.ResultsExported
import eu.semberal.dbstress.actor.ResultsExporterActor.ExportResults
import eu.semberal.dbstress.model.Results.UnitResult
import org.duh.resource._
import org.joda.time.DateTime._
import play.api.libs.json.Json
import eu.semberal.dbstress.model.JsonSupport._

class ResultsExporterActor(outputDir: File) extends Actor {
  val curr = Defaults.filePathFriendlyDateTimeFormat.print(now())

  override def receive: Receive = {

    case ExportResults(unitResults) =>
      try {
        writeCompleteJson(unitResults)
        sender ! ResultsExported
      } catch {
        case e: Throwable => sender ! Status.Failure(e)
      }

  }

  def writeCompleteJson(unitResults: List[UnitResult]): Unit = {
    for (b <- new BufferedWriter(new FileWriter(s"${outputDir}${File.separator}complete.$curr.json")).auto) {
      val out = Json.prettyPrint(Json.toJson(unitResults))
      b.write(out)
    }
  }

  def writeCsvSummary(): Unit = {
    // todo implement
    //      for (f <- new BufferedWriter(new FileWriter(s"${dir}${File.separator}summary.$curr.csv")).auto) {
    //
    //        val header = IndexedSeq("name", "description",
    //          "expectedDbCalls", "executedDbCalls", "notExecutedDbCalls", "successfulDbCalls", "failedDbCalls",
    //          "executedDbCallsMin", "executedDbCallsMax", "executedDbCallsMean", "executedDbCallsMedian", "executedDbCallsStddev",
    //          "successfulDbCallsMin", "successfulDbCallsMax", "successfulDbCallsMean", "successfulDbCallsMedian", "successfulDbCallsStddev",
    //          "failedDbCallsMin", "failedDbCallsMax", "failedDbCallsMean", "failedDbCallsMedian", "failedDbCallsStddev"
    //        )
    //
    //        val rows = List(unitResults.map(s =>
    //          IndexedSeq(s.unitConfig.name, s.unitConfig.description,
    //            s.summary.expectedDbCalls.toString, s.summary.executedDbCalls.toString, s.summary.notExecutedDbCalls.toString,
    //            s.summary.successfulDbCalls.toString, s.summary.failedDbCalls.toString,
    //
    //            s.summary.executedDbCallsMin.getOrMissingString, s.summary.executedDbCallsMax.getOrMissingString,
    //            s.summary.executedDbCallsMean.getOrMissingString, s.summary.executedDbCallsMedian.getOrMissingString,
    //            s.summary.executedDbCallsStddev.getOrMissingString,
    //
    //            s.summary.successfulDbCallsMin.getOrMissingString, s.summary.successfulDbCallsMax.getOrMissingString,
    //            s.summary.successfulDbCallsMean.getOrMissingString, s.summary.successfulDbCallsMedian.getOrMissingString,
    //            s.summary.successfulDbCallsStddev.getOrMissingString,
    //
    //            s.summary.failedDbCallsMin.getOrMissingString, s.summary.failedDbCallsMax.getOrMissingString,
    //            s.summary.failedDbCallsMean.getOrMissingString, s.summary.failedDbCallsMedian.getOrMissingString,
    //            s.summary.failedDbCallsStddev.getOrMissingString
    //          )
    //        ): _*)
    //
    //        CSVWriter.write(f, header :: rows)
    //      }
  }
}


object ResultsExporterActor {

  case class ExportResults(unitResults: List[UnitResult])

  // todo wrap in a case class
}
