package eu.semberal.dbstress.util

import java.io.{BufferedWriter, FileWriter}

import com.typesafe.scalalogging.slf4j.LazyLogging
import eu.semberal.dbstress.model.Results._
import org.duh.resource._
import play.api.libs.json.Json
import eu.semberal.dbstress.model.JsonSupport._

trait ResultsExporter {
  this: LazyLogging =>

  def exportResults(dir: String, unitResults: List[UnitResult]): Unit = {

    def writeCompleteJson(): Unit = {
      for (b <- new BufferedWriter(new FileWriter(s"$dir/complete.json")).auto) {
        val out = Json.prettyPrint(Json.toJson(unitResults))
        b.write(out)
      }
    }

    //    /* Write results in a csv file */
    //    for (b <- new BufferedWriter(new FileWriter(s"$dir/result.csv")).auto) {
    //      // todo configurable
    //      val header = List("name", "total_queries_count",
    //        "succ_queries", "succ_%", "succ_min", "succ_max", "succ_mean", "succ_median", "succ_stddev",
    //        "fail_queries", "fail_%", "fail_min", "fail_max", "fail_mean", "fail_median", "fail_stddev")
    //      b.write(header.mkString(","))
    //      b.newLine()
    //      unitResults foreach { result =>
    //        val s = List(
    //          result.name, result.unitRunResults.map(_.callResults).flatten.size, // todo do not flatten, figure out how to display this
    //
    //          result.successes.size, result.percentSuccess.getOrMissingString,
    //          result.succDurations.minimum.getOrMissingString, result.succDurations.maximum.getOrMissingString,
    //          result.succDurations.mean.getOrMissingString, result.succDurations.median.getOrMissingString,
    //          result.succDurations.stddev.getOrMissingString,
    //
    //          result.failures.size, result.percentFailure.getOrMissingString,
    //          result.failDurations.minimum.getOrMissingString, result.failDurations.maximum.getOrMissingString,
    //          result.failDurations.mean.getOrMissingString, result.failDurations.median.getOrMissingString,
    //          result.failDurations.stddev.getOrMissingString).mkString(",")
    //        b.write(s)
    //        b.newLine()
    //      }
    //    }
    //
    //    /* Write exceptions */
    //    for (b <- new BufferedWriter(new FileWriter(s"$dir/exceptions.csv")).auto) {
    //      // todo configurable
    //      for {u <- unitResults
    //           e <- u.exceptionMessages} {
    //        b.write(e.getMessage)
    //        b.newLine()
    //      }
    //    }

    writeCompleteJson()
  }

}
