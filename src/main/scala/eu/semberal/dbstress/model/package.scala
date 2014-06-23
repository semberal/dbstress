package eu.semberal.dbstress

import breeze.stats._

package object model {

  case class Scenario(units: Seq[TestUnit])

  case class TestUnit(name: String, config: TestUnitConfig, parallelConnections: Int)

  case class TestUnitConfig(dbConfig: DbConfig, repeats: Int)

  case class DbConfig(uri: String, driverClass: String, username: String, password: String, query: String)

  case class UnitResults()

  sealed trait DbResult {
    val start: Long
    val finish: Long
  }

  case class DbSuccess(start: Long, finish: Long, noOfResults: Int) extends DbResult

  case class DbFailure(start: Long, finish: Long, e: Throwable) extends DbResult

  case class UnitResult(name: String, dbResults: List[DbResult]) {
    lazy val successes = dbResults.collect({ case e: DbSuccess => e})
    lazy val failures = dbResults.collect({ case e: DbFailure => e})

    lazy val percentSuccess: Option[Double] = calcualtePercent(successes.length, dbResults.length)

    lazy val percentFailure = calcualtePercent(failures.length, dbResults.length)

    lazy val avgDuration = calculateAvgDuration(dbResults)

    lazy val avgSuccessDuration = calculateAvgDuration(successes)

    lazy val avgFailedDuration = calculateAvgDuration(failures)

    lazy val exceptionMessages = failures.map(_.e)

    private def calculateAvgDuration(l: List[DbResult]): Option[Double] =
      if (l.isEmpty) None else Some(mean(l.map(x => x.finish - x.start.toDouble)))

    private def calcualtePercent(x1: Long, x2: Long): Option[Double] = if (x2 == 0) None else Some(x1 / x2.toDouble * 100)
  }

}
