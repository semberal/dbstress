package eu.semberal.dbstress

import java.time.format.DateTimeFormatter

import scala.concurrent.duration.{DurationInt, FiniteDuration}

object Utils {
  val filePathFriendlyDateTimeFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

  val ScenarioTimeout: FiniteDuration = (24 * 60 * 60).seconds

  val ActorSystemShutdownTimeout: FiniteDuration = 20.seconds

  def toMillis(start: Long, end: Long): Long =
    Math.round((end - start) / 1000000.0)

}
