package eu.semberal.dbstress.config

import java.io.BufferedReader
import java.util.{Map => JMap}

import better.files._
import eu.semberal.dbstress.model.Configuration._
import org.yaml.snakeyaml.Yaml

import scala.jdk.CollectionConverters._
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

object ConfigParser {

  private def sequence[A, B](s: Seq[Either[A, B]]): Either[A, Seq[B]] =
    s.foldRight(Right(Nil): Either[A, List[B]]) { (e, acc) =>
      for (xs <- acc; x <- e) yield x :: xs
    }

  def parseConfigurationYaml(
      f: File,
      defaultPassword: Option[String]
  ): Either[String, ScenarioConfig] =
    parseConfigurationYaml(f.bufferedReader, defaultPassword)

  def parseConfigurationYaml(
      reader: Dispose[BufferedReader],
      defaultPassword: Option[String]
  ): Either[String, ScenarioConfig] = {

    def isStringNonEmpty(s: String): Boolean =
      Option(s).getOrElse("").length > 0

    val yaml = new Yaml

    reader.map { reader =>
      Try {
        yaml
          .loadAll(reader)
          .asScala
          .map(x =>
            Map(x.asInstanceOf[JMap[String, Object]].asScala.toList: _*)
          )
      }
    } apply {
      case Failure(e) =>
        Left(e.getMessage)

      case Success(foo) =>
        val units = foo.toList.map { map =>
          for {
            uri <- loadFromMap[String, String](map, "uri")(isStringNonEmpty)
            driverClass <- loadFromMapOptional[String, String](
              map,
              "driver_class"
            )(isStringNonEmpty)
            username <- loadFromMap[String, String](map, "username")(
              isStringNonEmpty
            )
            password <- loadFromMap[String, String](
              map,
              "password",
              defaultPassword
            )()
            query <- loadFromMap[String, String](map, "query")(isStringNonEmpty)
            connectionTimeout <- loadFromMapOptional[Int, java.lang.Integer](
              map,
              "connection_timeout"
            )(_ > 0)

            repeats <- loadFromMap[Int, java.lang.Integer](map, "repeats")(
              _ > 0
            )

            unitName <- loadFromMap[String, String](map, "unit_name")(x =>
              isStringNonEmpty(x) && x.matches("[a-zA-Z0-9]+")
            )
            description <- loadFromMapOptional[String, String](
              map,
              "description"
            )()
            parallelConnections <- loadFromMap[Int, java.lang.Integer](
              map,
              "parallel_connections"
            )(_ > 0)
          } yield {
            val dbConfig = DbCommunicationConfig(
              uri,
              driverClass,
              username,
              password,
              query,
              connectionTimeout
            )

            val unitConfig = UnitRunConfig(dbConfig, repeats)

            UnitConfig(unitName, description, unitConfig, parallelConnections)
          }
        }

        sequence(units).flatMap(x => {
          val unitNames = x.map(_.name)
          if (unitNames.distinct.length != unitNames.length) {
            Left(
              "Unit names must be distinct, scenario configuration contains duplicate unit names"
            )
          } else if (unitNames.isEmpty) {
            Left("No units have been configured")
          } else {
            Right(ScenarioConfig(x))
          }
        })
    }
  }

  private[this] def loadFromMap[T, U: ClassTag](
      map: Map[String, Any],
      key: String,
      default: Option[T] = None
  )(
      validation: T => Boolean = (_: T) => true
  )(implicit ev: U => T): Either[String, T] = {
    loadFromMapOptional[T, U](map, key, default)(validation) match {
      case Right(None)    => Left(s"Configuration property $key is missing")
      case Left(x)        => Left(x)
      case Right(Some(x)) => Right(x)
    }
  }

  private[this] def loadFromMapOptional[T, U: ClassTag](
      map: Map[String, Any],
      key: String,
      default: Option[T] = None
  )(
      validationIfPresent: T => Boolean = (_: T) => true
  )(implicit ev: U => T): Either[String, Option[T]] = {
    val rtc = implicitly[ClassTag[U]].runtimeClass
    map.get(key) match {
      case None                                 => Right(default)
      case Some(x: U) if validationIfPresent(x) => Right(Some(x))
      case Some(x: U) =>
        Left(s"""Invalid value "$x" for configuration entry: "$key"""")
      case Some(x) =>
        Left(
          s"""Value "$x" does conform to the expected type: "${rtc.getSimpleName}""""
        )
    }
  }
}
