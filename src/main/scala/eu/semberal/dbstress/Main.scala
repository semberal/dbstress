package eu.semberal.dbstress

import java.io.File
import java.util.concurrent.TimeoutException

import akka.actor.ActorSystem
import ch.qos.logback.classic.{Level, Logger}
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import com.typesafe.scalalogging.LazyLogging
import eu.semberal.dbstress.config.ConfigParser.parseConfigurationYaml
import eu.semberal.dbstress.model.Results.{ConnectionInitException, UnitRunException}
import eu.semberal.dbstress.util.{CsvResultsExport, ResultsExport}
import org.slf4j.LoggerFactory
import scopt.OptionParser

import scala.concurrent.{Await, Future}

object Main extends LazyLogging {

  case class CmdLineArguments(configFile: File = null, outputDir: File = null,
                              dbPassword: Option[String] = None)

  def main(args: Array[String]): Unit = {
    val parser = new OptionParser[CmdLineArguments]("dbstress") {
      private val version = Option(getClass.getPackage.getImplementationVersion).getOrElse("unknown")

      head("dbstress", version, "Database performance and stress testing tool")

      opt[File]('c', "config").valueName("CONFIG_FILE").text("Path to the configuration YAML file").required().action { (x, c) =>
        c.copy(configFile = x)
      }.validate {
        case x if !x.exists() => failure(s"File '$x' does not exist")
        case x if !x.isFile => failure(s"'$x' is not a file")
        case x if !x.canRead => failure(s"File '$x' is not readable")
        case _ => success
      }

      opt[File]('o', "output").valueName("OUTPUT_DIR").text("Output directory").required().action { (x, c) =>
        c.copy(outputDir = x)
      }.validate {
        case x if !x.exists() => failure(s"Directory '$x' does not exist")
        case x if !x.isDirectory => failure(s"'$x' is not a directory")
        case x if !x.canWrite => failure(s"Directory '$x' is not writeable")
        case _ => success
      }

      opt[String]('p', "password").valueName("DB_PASSWORD").text("Password to be used for database connections across all units").action { (x, c) =>
        c.copy(dbPassword = Some(x))
      }

      opt[Unit]('v', "verbose").text("Verbose output").action { (_, c) =>
        LoggerFactory.getLogger("eu.semberal.dbstress").asInstanceOf[Logger].setLevel(Level.DEBUG)
        c
      }

      version("version").text("Show application version")
      help("help").text("Show help")

      override def showUsageOnError: Boolean = true
    }

    parser.parse(args, CmdLineArguments()) match {
      case Some(CmdLineArguments(configFile, outputDir, dbPassword)) =>
        parseConfigurationYaml(configFile, dbPassword) match {
          case Right(sc) =>
            logger.debug("Scenario configuration successfully loaded")
            val minConn: Int = sc.units.map(_.parallelConnections).sum
            logger.debug(s"Database worker threads count: $minConn")
            val key = "akka.dispatchers.db-dispatcher.thread-pool-executor.core-pool-size-min"
            val config = ConfigFactory.load().withValue(key, ConfigValueFactory.fromAnyRef(minConn))

            val system = ActorSystem("dbstressMaster", config)
            val exports: List[ResultsExport] = new CsvResultsExport(outputDir) :: Nil
            val future: Future[Unit] = new Orchestrator(system).run(sc, exports)
            val bugMsg = "This is most likely an application bug. Please, file an issue in the dbstress bug tracker"
            val exitStatus: Int = try {
              Await.result(future, Defaults.ScenarioTimeout)
              logger.info("Scenario finished successfully")
              0
            } catch {
              case e: ConnectionInitException =>
                logger.error(s"Some database connection could not be initialized, terminating the scenario")
                3
              case e: TimeoutException =>
                logger.error("Scenario has timed out")
                4
              case e: UnitRunException =>
                logger.error(s"Error during unit run execution. $bugMsg", e)
                10
              case e: Exception =>
                logger.error(s"Error during scenario execution. $bugMsg", e)
                11
            }

            try {
              Await.result(system.terminate(), Defaults.ActorSystemShutdownTimeout)
            } catch {
              case e: TimeoutException =>
                logger.warn("Unable to shutdown the actor system within the specified time limit", e)
              case e: Exception =>
                logger.warn("Exception during actor system shutdown has occurred", e)
            }
            if (exitStatus != 0) System.exit(exitStatus)
          case Left(msg) =>
            logger.info(s"Configuration error: $msg")
            System.exit(2)
        }
      case None => System.exit(1)
    }
  }
}
