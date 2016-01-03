package eu.semberal.dbstress

import org.joda.time.format.{DateTimeFormat, ISODateTimeFormat}

import scala.concurrent.duration.DurationInt

object Defaults {
  val dateTimeFormat = ISODateTimeFormat.dateTime()
  val filePathFriendlyDateTimeFormat = DateTimeFormat.forPattern("yyyyMMdd_HHmmss")

  val ScenarioTimeout = (24*60*60).seconds

  val ActorSystemShutdownTimeout = 20.seconds

}
