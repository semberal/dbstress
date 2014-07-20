package eu.semberal.dbstress

import java.io.File

import com.typesafe.scalalogging.slf4j.LazyLogging
import eu.semberal.dbstress.config.ConfigParser.parseConfigurationYaml
import scopt.OptionParser

object Main extends LazyLogging {

  case class CmdLineArguments(configFile: File = null, outputDir: File = null)

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
        case x => success
      }

      opt[File]('o', "output").valueName("OUTPUT_DIR").text("Output directory").required().action { (x, c) =>
        c.copy(outputDir = x)
      }.validate {
        case x if !x.exists() => failure(s"Directory '$x' does not exist")
        case x if !x.isDirectory => failure(s"'$x' is not a directory")
        case x if !x.canWrite => failure(s"Directory '$x' is not writeable")
        case x => success
      }

      version("version").text("Show application version")
      help("help").text("Show help")

      override def showUsageOnError: Boolean = true
    }

    parser.parse(args, CmdLineArguments()) match {
      case Some(CmdLineArguments(configFile, outpuDir)) => parseConfigurationYaml(configFile) match {
        case Right(sc) => new Orchestrator(configFile).run(sc)
        case Left(msg) =>
          System.err.println(s"Configuration error: $msg")
          System.exit(2) // exit status 2 when configuration parsing error has occurred
      }
      case None => System.exit(1) // exit status 1 when command line arguments were incorrect
    }
  }
}