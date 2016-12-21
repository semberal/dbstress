package eu.semberal.dbstress

import org.joda.time.format.DateTimeFormat

import scala.concurrent.duration.DurationInt

object Defaults {
  val filePathFriendlyDateTimeFormat = DateTimeFormat.forPattern("yyyyMMdd_HHmmss")

  val ScenarioTimeout = (24*60*60).seconds

  val ActorSystemShutdownTimeout = 20.seconds

}
