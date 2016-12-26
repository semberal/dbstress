package eu.semberal.dbstress.model

import java.io.File

object Configuration {

  case class UserConfiguration(configFile: File, outputDir: File)

  case class ScenarioConfig(units: Seq[UnitConfig])

  case class UnitConfig(name: String, description: Option[String], config: UnitRunConfig, parallelConnections: Int)

  case class UnitRunConfig(dbConfig: DbCommunicationConfig, repeats: Int)

  case class DbCommunicationConfig(uri: String, driverClass: Option[String],
                                   username: String, password: String, query: String,
                                   connectionTimeout: Option[Int])

  object DbCommunicationConfigWithoutPassword {
    def unapply(config: DbCommunicationConfig): Option[(String, Option[String], String, String, Option[Int])] =
      Some((config.uri, config.driverClass, config.username, config.query, config.connectionTimeout))
  }

}
