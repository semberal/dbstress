package eu.semberal.dbstress.model

object Configuration {

  /* Configuration object*/
  case class Scenario(units: Seq[UnitConfig])

  case class UnitConfig(name: String, config: UnitRunConfig, parallelConnections: Int)

  case class UnitRunConfig(dbConfig: DbCommunicationConfig, repeats: Int)

  case class DbCommunicationConfig(uri: String, driverClass: String,
                                   username: String, password: String, query: String,
                                   connectionTimeout: Int, queryTimeout: Int)

}
