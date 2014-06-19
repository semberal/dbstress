package eu.semberal.dbstress

package object model {

  case class Scenario(units: Seq[TestUnit])

  case class TestUnit(name: String, config: TestUnitConfig, parallelConnections: Int)

  case class TestUnitConfig(dbConfig: DbConfig, repeats: Int)

  case class DbConfig(uri: String, username: String, password: String, query: String)

  case class UnitResults()

  sealed trait DbResult {
    val start: Long
    val finish: Long
  }

  case class DbSuccess(start: Long, finish: Long, noOfResults: Int) extends DbResult

  case class DbFailure(start: Long, finish: Long, e: Throwable) extends DbResult

  case class UnitResult(name: String, dbResults: List[DbResult]) {
    lazy val successes = dbResults.filter(_.isInstanceOf[DbSuccess])
    lazy val failures = dbResults.filter(_.isInstanceOf[DbFailure])

    // todo handle empty dbResults list
    lazy val percentSuccess: Double = successes.length / dbResults.length.asInstanceOf[Double] * 100

    lazy val percentFailure = failures.length / dbResults.length.asInstanceOf[Double] * 100

    lazy val avgDuration = calculateAvgDuration(dbResults)

    lazy val avgSuccessDuration = calculateAvgDuration(successes)

    lazy val avgFailedDuration = calculateAvgDuration(failures)

    private def calculateAvgDuration(l: List[DbResult]): Option[Double] = { // todo proper statistics library
      val listLength = l.length
      if(listLength == 0) None else {
        val m = l.map(x => x.finish - x.start)
        Some(m.sum / m.length.asInstanceOf[Double])
      }
    }
  }
}
