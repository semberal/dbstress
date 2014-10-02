package eu.semberal.dbstress.config

import java.io.{BufferedReader, File, FileReader, Reader}
import java.util.{Map => JMap}

import eu.semberal.dbstress.model.Configuration._
import eu.semberal.dbstress.util.ModelExtensions._
import org.yaml.snakeyaml.Yaml
import resource._

import scala.collection.JavaConversions._
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

object ConfigParser {

  private def sequence[A, B](s: Seq[Either[A, B]]): Either[A, Seq[B]] =
    s.foldRight(Right(Nil): Either[A, List[B]]) {
      (e, acc) => for (xs <- acc.right; x <- e.right) yield x :: xs
    }

  def parseConfigurationYaml(f: File): Either[String, ScenarioConfig] =
    parseConfigurationYaml(new BufferedReader(new FileReader(f)))

  def parseConfigurationYaml(reader: Reader): Either[String, ScenarioConfig] = {

    def isStringNonEmpty(s: String): Boolean = Option(s).getOrElse("").length > 0

    val yaml = new Yaml

    managed(reader).map { reader =>
      yaml.loadAll(reader).map(x => Map(x.asInstanceOf[JMap[String, Object]].toList: _*))
    }.toTry match {
      case Failure(e) => Left(e.getMessage)
      case Success(foo) =>
        val units = foo.toList.map { map =>
          for {
            uri <- loadFromMap[String, String](map, "uri")(isStringNonEmpty).right
            driverClass <- loadFromMapOptional[String, String](map, "driver_class")(isStringNonEmpty).right
            username <- loadFromMap[String, String](map, "username")(isStringNonEmpty).right
            password <- loadFromMap[String, String](map, "password")().right
            query <- loadFromMap[String, String](map, "query")(isStringNonEmpty).right
            connectionTimeout <- loadFromMapOptional[Int, java.lang.Integer](map, "connection_timeout")(_ > 0).right
            queryTimeout <- loadFromMapOptional[Int, java.lang.Integer](map, "query_timeout")(_ > 0).right

            repeats <- loadFromMap[Int, java.lang.Integer](map, "repeats")(_ > 0).right

            unitName <- loadFromMap[String, String](map, "unit_name")(x => isStringNonEmpty(x) && x.matches("[a-zA-Z0-9]+")).right
            description <- loadFromMapOptional[String, String](map, "description")().right
            parallelConnections <- loadFromMap[Int, java.lang.Integer](map, "parallel_connections")(_ > 0).right
          } yield {
            val dbConfig = DbCommunicationConfig(uri, driverClass, username, password, query,
              connectionTimeout, queryTimeout)

            val unitConfig = UnitRunConfig(dbConfig, repeats)

            UnitConfig(unitName, description, unitConfig, parallelConnections)
          }
        }

        sequence(units).right.flatMap(x => {
          val unitNames = x.map(_.name)
          if (unitNames.distinct.length != unitNames.length) {
            Left("Unit names must be distinct, scenario configuration contains duplicate unit names")
          } else if (unitNames.isEmpty) {
            Left("No units have been configured")
          } else {
            Right(ScenarioConfig(x))
          }
        })
    }
  }

  private[this] def loadFromMap[T, U <% T : ClassTag](map: Map[String, Any], key: String)
                                            (validation: T => Boolean = (_: T) => true): Either[String, T] = {
    loadFromMapOptional[T, U](map, key)(validation) match {
      case Right(None) => Left(s"Configuration property $key is missing")
      case Left(x) => Left(x)
      case Right(Some(x)) => Right(x)
    }
  }

  private[this] def loadFromMapOptional[T, U <% T : ClassTag](map: Map[String, Any], key: String)
                                                    (validationIfPresent: T => Boolean = (_: T) => true): Either[String, Option[T]] = {
    val rtc = implicitly[ClassTag[U]].runtimeClass
    map.get(key) match {
      case None => Right(None)
      case Some(x) if !rtc.isInstance(x) => Left( s"""Value "$x" does conform to the expected type: "${rtc.getSimpleName}"""")
      case Some(x: U) if !validationIfPresent(x) => Left( s"""Invalid value "$x" for configuration entry: "$key"""")
      case Some(x: U) => Right(Some(x))
    }
  }
}
