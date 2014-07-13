package eu.semberal.dbstress.config

import java.io.{BufferedReader, File, FileReader}
import java.util.{Map => JMap}

import eu.semberal.dbstress.model.Configuration._
import org.duh.resource._
import org.yaml.snakeyaml.Yaml

import scala.collection.JavaConversions._
import scala.reflect.ClassTag

object ConfigParser {

  private def sequence[A, B](s: Seq[Either[A, B]]): Either[A, Seq[B]] =
    s.foldRight(Right(Nil): Either[A, List[B]]) {
      (e, acc) => for (xs <- acc.right; x <- e.right) yield x :: xs
    }

  /*
  * todo Ensure unique unit name (actor have the same name => must be unique)
  * todo Values validation (e.g. repeats > 0, ...)
  */
  def parseConfigurationYaml(f: File): Either[String, ScenarioConfig] = {
    val yaml = new Yaml
    val units = for (reader <- new BufferedReader(new FileReader(f)).auto) yield {
      yaml.loadAll(reader).map(x => Map(x.asInstanceOf[JMap[String, Object]].toList: _*))
    }.map { map =>
      for {
        uri <- loadFromMap[String](map, "uri")()().right
        driverClass <- loadFromMap[String](map, "driver_class")()().right
        username <- loadFromMap[String](map, "username")()().right
        password <- loadFromMap[String](map, "password")()().right
        query <- loadFromMap[String](map, "query")()().right
        connectionTimeout <- loadFromMap[java.lang.Integer](map, "connection_timeout")()().right
        queryTimeout <- loadFromMap[java.lang.Integer](map, "query_timeout")()().right

        repeats <- loadFromMap[java.lang.Integer](map, "repeats")()().right

        unitName <- loadFromMap[String](map, "unit_name")()().right
        description <- loadFromMap[String](map, "description")()().right
        parallelConnections <- loadFromMap[java.lang.Integer](map, "parallel_connections")()().right
      } yield {
        val dbConfig = DbCommunicationConfig(uri, driverClass, username, password, query,
          connectionTimeout, queryTimeout)

        val unitConfig = UnitRunConfig(dbConfig, repeats)

        UnitConfig(unitName, description, unitConfig, parallelConnections)
      }
    }.toList

    sequence(units).right.map(ScenarioConfig)
  }

  def loadFromMap[T: ClassTag](map: Map[String, Any], key: String)
                              (validation: T => Boolean = (_: T) => true)
                              (validationMsg: String = ""): Either[String, T] = {
    val rtc = implicitly[ClassTag[T]].runtimeClass
    map.get(key) match {
      case None => Left(s"Configuration property $key is missing")
      case Some(x) if !rtc.isInstance(x) => Left(s"Value $x does conform to the expected type $rtc")
      case Some(x: T) if !validation(x) => Left(validationMsg)
      case Some(x: T) => Right(x)
    }
  }
}
