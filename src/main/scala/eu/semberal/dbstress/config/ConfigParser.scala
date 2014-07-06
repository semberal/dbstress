package eu.semberal.dbstress.config

import eu.semberal.dbstress.model.Configuration._
import org.duh.resource._
import org.yaml.snakeyaml.Yaml

import scala.collection.JavaConversions._

object ConfigParser {

  /*
  * todo Configurable config file (after command line options parsing is implemented)
  * todo Ensure unique unit name (actor have the same name => must be unique)
  * todo Values validation (e.g. repeats > 0, ...)
  */
  def parseConfigurationYaml(): Scenario = {
    val yaml = new Yaml
    val units = for (x <- this.getClass.getClassLoader.getResourceAsStream("config.yaml").auto) yield {
      yaml.loadAll(x).map(x => Map(x.asInstanceOf[java.util.Map[String, Object]].toList: _*))
    }.map(map => {
      val dbConfig = DbCommunicationConfig(map("uri").asInstanceOf[String], map("driver_class").asInstanceOf[String],
        map("username").asInstanceOf[String], map("password").asInstanceOf[String], map("query").asInstanceOf[String],
        map("connection_timeout").asInstanceOf[Int], map("query_timeout").asInstanceOf[Int])
      val unitConfig = UnitRunConfig(dbConfig, map("repeats").asInstanceOf[Int])
      UnitConfig(map("unit_name").asInstanceOf[String], map("description").asInstanceOf[String],
        unitConfig, map("parallel_connections").asInstanceOf[Int])
    }).toList
    Scenario(units)
  }
}
