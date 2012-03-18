package com.bimbr.clisson.server.config

import scala.collection.JavaConversions._

import org.junit.runner.RunWith
import org.specs2.execute.Result
import org.specs2.mutable.{ After, Specification }
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ConfigSpec extends Specification {
  "Config construction" should {
    "use specified properties file" in withConfig("test-server.properties") { cfg =>
      cfg("testProperty") mustEqual Some("test value")
    }
    "report an error if specified file can't be found" in {
      Config.fromPropertiesFile("nonexistent-server.properties") must beLeft.like {
        case errorMessage => errorMessage must contain ("nonexistent-server.properties") 
      }
    }
  }
  "Config" should {
    "interpolate ${variables} using system properties" in withConfig("test-server.properties") { cfg =>
      cfg("tmpDir") mustEqual Some(System.getProperty("java.io.tmpdir") + "/test")
    }
  }
  
  def withConfig(fileName: String)(check: Config => Result): Result = 
    Config.fromPropertiesFile("test-server.properties").fold(failure(_), check(_))
}

