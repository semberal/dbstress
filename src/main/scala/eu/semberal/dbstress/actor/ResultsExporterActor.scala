package eu.semberal.dbstress.actor

import java.io.{BufferedWriter, File, FileWriter}

import akka.actor.{Actor, Status}
import breeze.io.CSVWriter
import eu.semberal.dbstress.Defaults.filePathFriendlyDateTimeFormat
import eu.semberal.dbstress.actor.ManagerActor.ResultsExported
import eu.semberal.dbstress.actor.ResultsExporterActor.ExportResults
import eu.semberal.dbstress.model.JsonSupport._
import eu.semberal.dbstress.model.Results.ScenarioResult
import eu.semberal.dbstress.util.ModelExtensions._
import org.joda.time.DateTime._
import play.api.libs.json.Json
import resource._

class ResultsExporterActor(outputDir: File) extends Actor {

  override def receive: Receive = {

    case ExportResults(sr: ScenarioResult) =>
      try {
        writeResults(sr)
        sender ! ResultsExported
      } catch {
        case e: Throwable => sender ! Status.Failure(e)
      }

  }

  def writeResults(scenarioResult: ScenarioResult): Unit = {
    val curr = filePathFriendlyDateTimeFormat.print(now())

    def writeCompleteJson(): Unit = {
      for (b <- managed(new BufferedWriter(new FileWriter(s"${outputDir}${File.separator}complete.$curr.json")))) {
        val out = Json.prettyPrint(Json.toJson(scenarioResult))
        b.write(out)
      }
    }

    def writeCsvSummary(): Unit = {

      for (f <- managed(new BufferedWriter(new FileWriter(s"${outputDir}${File.separator}summary.$curr.csv")))) {

        val header = IndexedSeq(
          "name", "description", "uri", "parallelConnections", "repeats",

          "expectedDbCalls", "executedDbCalls", "successfulDbCalls", "failedDbCalls",

          "executedDbCallsMin", "executedDbCallsMax", "executedDbCallsMean", "executedDbCallsMedian", "executedDbCallsStddev",

          "successfulDbCallsMin", "successfulDbCallsMax", "successfulDbCallsMean", "successfulDbCallsMedian", "successfulDbCallsStddev",

          "failedDbCallsMin", "failedDbCallsMax", "failedDbCallsMean", "failedDbCallsMedian", "failedDbCallsStddev"
        )

        val rows = List(scenarioResult.unitResults.map(s =>
          IndexedSeq(
            s.unitConfig.name, s.unitConfig.description, s.unitConfig.config.dbConfig.uri,
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

        CSVWriter.write(f, header :: rows)
      }
    }

    writeCompleteJson()
    writeCsvSummary()
  }
}


object ResultsExporterActor {

  case class ExportResults(scenarioResult: ScenarioResult)

}
