package eu.semberal.dbstress

package object model {

  case class Scenario(units: Seq[TestUnit])

  case class TestUnit(name: String, config: TestUnitConfig, parallelUsers: Int)

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
    private val success: DbResult => Boolean = _.isInstanceOf[DbSuccess]
    private val failure: DbResult => Boolean = !success(_)

    lazy val percentSuccess: Double =
      dbResults.count(success) / dbResults.length.asInstanceOf[Double] * 100

    lazy val percentFailure = 100.0 - percentSuccess

    lazy val avgDuration: Double = {
      val l = dbResults.map(x => x.finish - x.start)//.filter(_ > 0)
      l.sum / l.length.asInstanceOf[Double]
    }
  }
}
