package eu.semberal.dbstress.config

import java.io.{BufferedReader, File, FileReader}
import java.util.{Map => JMap}

import eu.semberal.dbstress.model.Configuration._
import org.duh.resource._
import org.yaml.snakeyaml.Yaml

import scala.collection.JavaConversions._
import scala.reflect.ClassTag
import scala.util.{Success, Try}

object ConfigParser {

  /*
  * todo Ensure unique unit name (actor have the same name => must be unique)
  * todo Values validation (e.g. repeats > 0, ...)
  */
  def parseConfigurationYaml(f: File): Try[ScenarioConfig] = {
    val yaml = new Yaml
    val units = for (reader <- new BufferedReader(new FileReader(f)).auto) yield {
      yaml.loadAll(reader).map(x => Map(x.asInstanceOf[JMap[String, Object]].toList: _*))
    }.map(map => {
      val uri = map("uri").asInstanceOf[String]

      val driverClass = loadFromMapWithCheck[String](map, "driver_class")
      val username = loadFromMapWithCheck[String](map, "username")
      val password = loadFromMapWithCheck[String](map, "password")
      val query = loadFromMapWithCheck[String](map, "query")
      val connectionTimeout = loadFromMapWithCheck[Int](map, "connection_timeout")
      val queryTimeout = loadFromMapWithCheck[Int](map, "query_timeout")

      val repeats = loadFromMapWithCheck[Int](map, "repeats")

      val unitName = loadFromMapWithCheck[String](map, "unit_name")
      val description = loadFromMapWithCheck[String](map, "description")
      val parallelConnections = loadFromMapWithCheck[String](map, "parallel_connections")

      val s = List(driverClass, username, password, query, connectionTimeout, queryTimeout) ++
        List(repeats) ++ List(unitName, description, parallelConnections)

      import scalaz._
      import Scalaz._

      val s1 = s.sequenceU // todo check if is correct

      val dbConfig = DbCommunicationConfig(uri, driverClass, username, password, query,
        connectionTimeout, queryTimeout)




      val unitConfig = UnitRunConfig(dbConfig, repeats)



      UnitConfig(unitName, description, unitConfig, parallelConnections)
    }).toList

    Success(ScenarioConfig(units))

  }


  def loadFromMapWithCheck[T: ClassTag](map: Map[String, Any], key: String): Either[String, T] = {
    val rtc = implicitly[ClassTag[T]].runtimeClass
    map.get(key) match {
      case None => Left(s"Configuration property $key is missing")
      case Some(x) if rtc.isInstance(x) =>
        Right(x.asInstanceOf[T])
      case Some(x) =>
        Left(s"Value x does conform to the expected type $rtc")
    }
  }
}
