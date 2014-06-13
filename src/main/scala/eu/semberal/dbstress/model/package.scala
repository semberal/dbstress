package eu.semberal.dbstress

package object model {

  case class Scenario(units: Seq[TestUnit], repeats: Int)

  case class TestUnit(config: TestUnitConfig, parallelUsers: Int)

  case class TestUnitConfig(jdbcUri: String, username: String, password: String, query: String)

  case class UnitResults()

  case class UnitRunResult(success: Boolean, start: Long, finish: Long, noOfResults: Int)

  case class ScenarioResults()

}
