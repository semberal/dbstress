package eu.semberal.dbstress.config

import java.io.InputStreamReader

import org.scalatest.{Matchers, FlatSpec}
import scala.reflect.ClassTag
import resource._

class ConfigParserTest extends FlatSpec with Matchers {

  "ConfigParser" should "correctly reject an unit with non-alphanumeric characters in the name" in {
    val stream = this.getClass.getClassLoader.getResourceAsStream("test_config2.yaml")
    managed(new InputStreamReader(stream)).foreach { reader =>
      val result = ConfigParser.parseConfigurationYaml(reader)
      result should be(Left(s"""Invalid value "Foo Bar" for configuration entry: "unit_name""""))
    }
  }
}
