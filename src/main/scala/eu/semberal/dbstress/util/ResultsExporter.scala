package eu.semberal.dbstress.util

import java.io.{BufferedWriter, File, FileWriter}

import breeze.io.CSVWriter
import com.typesafe.scalalogging.slf4j.LazyLogging
import eu.semberal.dbstress.Defaults
import eu.semberal.dbstress.model.JsonSupport._
import eu.semberal.dbstress.model.Results._
import eu.semberal.dbstress.util.ModelExtensions._
import org.duh.resource._
import org.joda.time.DateTime
import org.joda.time.DateTime.now
import play.api.libs.json.Json

trait ResultsExporter {
  this: LazyLogging =>

  def exportResults(dir: String, unitResults: List[UnitResult]): Unit = {

    val curr = Defaults.filePathFriendlyDateTimeFormat.print(now())

    def writeCompleteJson(): Unit = {
      for (b <- new BufferedWriter(new FileWriter(s"${dir}${File.separator}complete.$curr.json")).auto) {
        val out = Json.prettyPrint(Json.toJson(unitResults))
        b.write(out)
      }
    }

    def writeCsvSummary(): Unit = {
      for (f <- new BufferedWriter(new FileWriter(s"${dir}${File.separator}summary.$curr.csv")).auto) {

        val header = IndexedSeq("name", "description",
          "expectedDbCalls", "executedDbCalls", "notExecutedDbCalls", "successfulDbCalls", "failedDbCalls",
          "executedDbCallsMin", "executedDbCallsMax", "executedDbCallsMean", "executedDbCallsMedian", "executedDbCallsStddev",
          "successfulDbCallsMin", "successfulDbCallsMax", "successfulDbCallsMean", "successfulDbCallsMedian", "successfulDbCallsStddev",
          "failedDbCallsMin", "failedDbCallsMax", "failedDbCallsMean", "failedDbCallsMedian", "failedDbCallsStddev"
        )

        val rows = List(unitResults.map(s =>
          IndexedSeq(s.unitConfig.name, s.unitConfig.description,
            s.summary.expectedDbCalls.toString, s.summary.executedDbCalls.toString, s.summary.notExecutedDbCalls.toString,
            s.summary.successfulDbCalls.toString, s.summary.failedDbCalls.toString,

            s.summary.executedDbCallsMin.getOrMissingString, s.summary.executedDbCallsMax.getOrMissingString,
            s.summary.executedDbCallsMean.getOrMissingString, s.summary.executedDbCallsMedian.getOrMissingString,
            s.summary.executedDbCallsStddev.getOrMissingString,

            s.summary.successfulDbCallsMin.getOrMissingString, s.summary.successfulDbCallsMax.getOrMissingString,
            s.summary.successfulDbCallsMean.getOrMissingString, s.summary.successfulDbCallsMedian.getOrMissingString,
            s.summary.successfulDbCallsStddev.getOrMissingString,

            s.summary.failedDbCallsMin.getOrMissingString, s.summary.failedDbCallsMax.getOrMissingString,
            s.summary.failedDbCallsMean.getOrMissingString, s.summary.failedDbCallsMedian.getOrMissingString,
            s.summary.failedDbCallsStddev.getOrMissingString
          )
        ): _*)

        CSVWriter.write(f, header :: rows)
      }
    }

    writeCompleteJson()
    writeCsvSummary()
  }
}
