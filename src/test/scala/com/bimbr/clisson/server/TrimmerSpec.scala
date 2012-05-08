package com.bimbr.clisson.server

import java.util.{ Timer, TimerTask }
import com.bimbr.clisson.server.config.Config
import org.junit.runner.RunWith
import org.mockito.Matchers.{ eq => meq }
import org.specs2.execute.Result
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class TrimmerSpec extends Specification with Mockito {
  "trimmer" should {
    "not schedule any tasks if trimming is disabled" in startTrimmerWithConfig(ConfigWithTrimmingDisabled) { timer => 
      there were noMoreCallsTo (timer)
    }
    "schedule trimming task at specified rate" in startTrimmerWithConfig(config(
        enabled   = Some("true"), 
        retention = Some("1 day"), 
        frequency = Some("1 hour"))) { timer =>
      there was one (timer).scheduleAtFixedRate(any[TimerTask], meq(0L), meq(3600000L)) toResult
    }
  }
  
  def config(enabled: Option[String], retention: Option[String], frequency: Option[String]): Config = {
    val config = mock[Config]
    config("trimming.enabled") returns enabled
    config("trimming.trimEventsOlderThan") returns retention
    config("trimming.frequency") returns frequency
    config
  }
  
  val ConfigWithTrimmingDisabled = config(Some("false"), Some("1 day"), Some("1 hour"))
  
  def startTrimmerWithConfig(config: Config)(body: Timer => Result): Result = {
    val timer = mock[Timer]
    val trimmer = new Trimmer(timer)
    trimmer.start(config)
    body(timer)
  }
}