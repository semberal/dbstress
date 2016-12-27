package eu.semberal.dbstress

import java.time.format.DateTimeFormatter

import scala.concurrent.duration.DurationInt

object Defaults {
  val filePathFriendlyDateTimeFormat = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

  val ScenarioTimeout = (24*60*60).seconds

  val ActorSystemShutdownTimeout = 20.seconds

}
