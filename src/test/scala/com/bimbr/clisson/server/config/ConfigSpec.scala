package com.bimbr.clisson.server.config

import scala.collection.JavaConversions._

import org.junit.runner.RunWith
import org.specs2.mutable.{ After, Specification }
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ConfigSpec extends Specification {
  "Config construction" should {
    "use specified properties file" in {
      Config.fromPropertiesFile("test-server.properties").fold(
          failure(_),
          cfg => cfg("testProperty") mustEqual Some("test value")
      )
    }
  }
}

