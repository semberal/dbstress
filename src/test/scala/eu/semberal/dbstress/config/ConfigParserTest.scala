package eu.semberal.dbstress.config

import java.io.{BufferedReader, InputStreamReader}

import better.files._
import org.scalatest.flatspec.AnyFlatSpec

class ConfigParserTest extends AnyFlatSpec {

  "ConfigParser" should "correctly reject an unit with non-alphanumeric characters in the name" in {
    val stream = this.getClass.getClassLoader.getResourceAsStream("test_config2.yaml")
    val result = ConfigParser.parseConfigurationYaml(new BufferedReader(new InputStreamReader(stream)).autoClosed, None)
    assert(result === Left(s"""Invalid value "Foo Bar" for configuration entry: "unit_name""""))
  }
}
