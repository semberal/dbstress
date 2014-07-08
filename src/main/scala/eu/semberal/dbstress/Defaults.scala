package eu.semberal.dbstress

import org.joda.time.format.{DateTimeFormat, ISODateTimeFormat}

object Defaults {
  val dateTimeFormat = ISODateTimeFormat.dateTime()
  val filePathFriendlyDateTimeFormat = DateTimeFormat.forPattern("yyyyMMdd_HHmmss")

}
