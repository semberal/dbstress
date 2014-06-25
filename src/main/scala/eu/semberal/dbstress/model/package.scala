
package eu.semberal.dbstress

package object model {

  case class Scenario(units: Seq[UnitConfig])

  case class UnitConfig(name: String, config: UnitRunConfig, parallelConnections: Int)

  case class UnitRunConfig(dbConfig: DbCommunicationConfig, repeats: Int)

  case class DbCommunicationConfig(uri: String, driverClass: String, username: String, password: String, query: String)

  sealed trait DbResult {
    val start: Long
    val finish: Long
  }

  case class DbSuccess(start: Long, finish: Long, noOfResults: Int) extends DbResult

  case class DbFailure(start: Long, finish: Long, e: Throwable) extends DbResult

  type UnitRunResult = List[DbResult]

  case class UnitResult(name: String, unitRunResults: List[UnitRunResult]) {
    lazy val flattened: List[DbResult] = unitRunResults.flatten // todo do not flatten, loses information

    private val dbResultsToDurations: DbResult => Double = x => x.finish.toDouble - x.start
    lazy val successes = flattened.collect({ case e: DbSuccess => e})
    lazy val failures = flattened.collect({ case e: DbFailure => e})

    lazy val succDurations = successes.map(dbResultsToDurations)
    lazy val failDurations = failures.map(dbResultsToDurations)

    lazy val percentSuccess: Option[Double] = calcualtePortion(successes.size, flattened.size)

    lazy val percentFailure = calcualtePortion(failures.size, flattened.size)

    lazy val exceptionMessages = failures.map(_.e)

    private def calcualtePortion(x1: Long, x2: Long): Option[Double] =
      if (x2 == 0) None else Some(x1 / x2.toDouble * 100)
  }

}
