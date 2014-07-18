package eu.semberal.dbstress

import java.io.File
import java.nio.file.Files.createTempDirectory

import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

class MainTest extends FlatSpec with Matchers with BeforeAndAfter {

  def withTempDir(testCode: File => Any) {
    val file = createTempDirectory("dbstress_MainTest_")
    try {
      testCode(file.toFile)
    } finally {
      
    }
  }

  "dbstress" should "foo" in withTempDir { f =>

  }

}

