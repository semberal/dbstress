package eu.semberal.dbstress.config

import org.scalatest.{Matchers, FlatSpec}
import scala.reflect.ClassTag

class ConfigParserTest extends FlatSpec with Matchers {
  "ClassTag" should "behave as expected for int type" in {
    val rtc = implicitly[ClassTag[java.lang.Integer]].runtimeClass
    //    val rtc = implicitly[ClassTag[Int]].runtimeClass
    rtc.isInstance(4) should be(true)
    rtc.isInstance(new java.lang.Integer(45)) should be(true)
    rtc.isInstance(null) should be(false)
  }

  it should "behave as expected for String type" in {
    val rtc = implicitly[ClassTag[String]].runtimeClass
    rtc.isInstance("foobar") should be(true)
    rtc.isInstance("") should be(true)
    rtc.isInstance(null) should be(false)
  }
}
