package eu.semberal.dbstress.util

import java.time.LocalDateTime

import com.github.tototoshi.csv.CSVWriter
import com.typesafe.scalalogging.LazyLogging
import eu.semberal.dbstress.Utils._
import eu.semberal.dbstress.model.Results.ScenarioResult
import eu.semberal.dbstress.util.ModelExtensions._
import better.files.File

trait ResultsExport {
  protected val curr: String =
    filePathFriendlyDateTimeFormat.format(LocalDateTime.now())

  def export(sr: ScenarioResult): Unit
}

class CsvResultsExport(outputDir: File) extends ResultsExport with LazyLogging {
  override def export(sr: ScenarioResult): Unit = {
    val csvFile = outputDir / s"summary.$curr.csv"

    logger.info(s"Exporting results to $csvFile")

    val header = IndexedSeq(
      "name",
      "description",
      "uri",
      "parallelConnections",
      "repeats",
      "connectionInitsMin",
      "connectionInitsMax",
      "connectionInitsMean",
      "connectionInitsMedian",
      "connectionInitsP90",
      "connectionInitsP99",
      "connectionInitsStddev",
      "expectedDbCalls",
      "executedDbCalls",
      "successfulDbCalls",
      "failedDbCalls",
      "executedDbCallsMin",
      "executedDbCallsMax",
      "executedDbCallsMean",
      "executedDbCallsMedian",
      "executedDbCallsP90",
      "executedDbCallsP99",
      "executedDbCallsStddev",
      "successfulDbCallsMin",
      "successfulDbCallsMax",
      "successfulDbCallsMean",
      "successfulDbCallsMedian",
      "successfulDbCallsP90",
      "successfulDbCallsP99",
      "successfulDbCallsStddev",
      "failedDbCallsMin",
      "failedDbCallsMax",
      "failedDbCallsMean",
      "failedDbCallsMedian",
      "failedDbCallsP90",
      "failedDbCallsP99",
      "failedDbCallsStddev"
    )

    val rows = List(
      sr.unitResults.map(s =>
        IndexedSeq(
          s.unitConfig.name,
          s.unitConfig.description.getOrElse(""),
          s.unitConfig.config.dbConfig.uri,
          s.unitConfig.parallelConnections.toString,
          s.unitConfig.config.repeats.toString,
          s.summary.connectionInitsSummary.min.getOrMissingString,
          s.summary.connectionInitsSummary.max.getOrMissingString,
          s.summary.connectionInitsSummary.mean.getOrMissingString,
          s.summary.connectionInitsSummary.median.getOrMissingString,
          s.summary.connectionInitsSummary.p90.getOrMissingString,
          s.summary.connectionInitsSummary.p99.getOrMissingString,
          s.summary.connectionInitsSummary.stddev.getOrMissingString,
          s.summary.expectedDbCalls.toString,
          s.summary.executedDbCallsSummary.count.toString,
          s.summary.successfulDbCallsSummary.count.toString,
          s.summary.failedDbCallsSummary.count.toString,
          s.summary.executedDbCallsSummary.min.getOrMissingString,
          s.summary.executedDbCallsSummary.max.getOrMissingString,
          s.summary.executedDbCallsSummary.mean.getOrMissingString,
          s.summary.executedDbCallsSummary.median.getOrMissingString,
          s.summary.executedDbCallsSummary.p90.getOrMissingString,
          s.summary.executedDbCallsSummary.p99.getOrMissingString,
          s.summary.executedDbCallsSummary.stddev.getOrMissingString,
          s.summary.successfulDbCallsSummary.min.getOrMissingString,
          s.summary.successfulDbCallsSummary.max.getOrMissingString,
          s.summary.successfulDbCallsSummary.mean.getOrMissingString,
          s.summary.successfulDbCallsSummary.median.getOrMissingString,
          s.summary.successfulDbCallsSummary.p90.getOrMissingString,
          s.summary.successfulDbCallsSummary.p99.getOrMissingString,
          s.summary.successfulDbCallsSummary.stddev.getOrMissingString,
          s.summary.failedDbCallsSummary.min.getOrMissingString,
          s.summary.failedDbCallsSummary.max.getOrMissingString,
          s.summary.failedDbCallsSummary.mean.getOrMissingString,
          s.summary.failedDbCallsSummary.median.getOrMissingString,
          s.summary.failedDbCallsSummary.p90.getOrMissingString,
          s.summary.failedDbCallsSummary.p99.getOrMissingString,
          s.summary.failedDbCallsSummary.stddev.getOrMissingString
        )
      ): _*
    )

    CSVWriter.open(csvFile.toJava).writeAll(header :: rows)
  }
}
