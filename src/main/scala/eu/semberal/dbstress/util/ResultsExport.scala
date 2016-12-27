package eu.semberal.dbstress.util

import java.io.{BufferedWriter, File, FileWriter, Writer}
import java.time.LocalDateTime

import com.github.tototoshi.csv.CSVWriter
import eu.semberal.dbstress.Defaults._
import eu.semberal.dbstress.model.Results.ScenarioResult
import eu.semberal.dbstress.util.ModelExtensions._
import resource._

trait ResultsExport {
  protected val curr = filePathFriendlyDateTimeFormat.format(LocalDateTime.now())

  def export(sr: ScenarioResult): Unit

  protected def withWriter(filePath: String)(op: Writer => Unit): Unit =
    for (b <- managed(new BufferedWriter(new FileWriter(filePath)))) op(b)

}

class CsvResultsExport(outputDir: File) extends ResultsExport {
  override def export(sr: ScenarioResult): Unit = withWriter(s"${outputDir}${File.separator}summary.$curr.csv") { w =>

    val header = IndexedSeq(
      "name", "description", "uri", "parallelConnections", "repeats",

      "expectedDbCalls", "executedDbCalls", "successfulDbCalls", "failedDbCalls",

      "executedDbCallsMin", "executedDbCallsMax", "executedDbCallsMean", "executedDbCallsMedian", "executedDbCallsStddev",

      "successfulDbCallsMin", "successfulDbCallsMax", "successfulDbCallsMean", "successfulDbCallsMedian", "successfulDbCallsStddev",

      "failedDbCallsMin", "failedDbCallsMax", "failedDbCallsMean", "failedDbCallsMedian", "failedDbCallsStddev"
    )

    val rows = List(sr.unitResults.map(s =>
      IndexedSeq(
        s.unitConfig.name, s.unitConfig.description.getOrElse(""), s.unitConfig.config.dbConfig.uri,
        s.unitConfig.parallelConnections.toString, s.unitConfig.config.repeats.toString,

        s.summary.expectedDbCalls.toString, s.summary.executedDbCallsSummary.count.toString, s.summary.successfulDbCallsSummary.count.toString,
        s.summary.failedDbCallsSummary.count.toString,

        s.summary.executedDbCallsSummary.min.getOrMissingString, s.summary.executedDbCallsSummary.max.getOrMissingString,
        s.summary.executedDbCallsSummary.mean.getOrMissingString, s.summary.executedDbCallsSummary.median.getOrMissingString,
        s.summary.executedDbCallsSummary.stddev.getOrMissingString,

        s.summary.successfulDbCallsSummary.min.getOrMissingString, s.summary.successfulDbCallsSummary.max.getOrMissingString,
        s.summary.successfulDbCallsSummary.mean.getOrMissingString, s.summary.successfulDbCallsSummary.median.getOrMissingString,
        s.summary.successfulDbCallsSummary.stddev.getOrMissingString,

        s.summary.failedDbCallsSummary.min.getOrMissingString, s.summary.failedDbCallsSummary.max.getOrMissingString,
        s.summary.failedDbCallsSummary.mean.getOrMissingString, s.summary.failedDbCallsSummary.median.getOrMissingString,
        s.summary.failedDbCallsSummary.stddev.getOrMissingString
      )
    ): _*)

    CSVWriter.open(w).writeAll(header :: rows)
  }
}
