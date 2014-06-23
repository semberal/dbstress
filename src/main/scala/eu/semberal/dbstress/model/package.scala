package eu.semberal.dbstress

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
    val dbResultsToDurations: DbResult => Double = x => x.finish.toDouble - x.start
    lazy val successes = dbResults.collect({ case e: DbSuccess => e})
    lazy val failures = dbResults.collect({ case e: DbFailure => e})

    lazy val succDurations = successes.map(dbResultsToDurations)
    lazy val failDurations = failures.map(dbResultsToDurations)

    lazy val percentSuccess: Option[Double] = calcualtePortion(successes.length, dbResults.length)

    lazy val percentFailure = calcualtePortion(failures.length, dbResults.length)

    lazy val exceptionMessages = failures.map(_.e)

    private[this] def calcualtePortion(x1: Long, x2: Long): Option[Double] =
      if (x2 == 0) None else Some(x1 / x2.toDouble * 100)
  }
}
