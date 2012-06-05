package com.bimbr.clisson.server

import java.util.{ Timer, TimerTask }
import com.typesafe.config.Config
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
        enabled   = true, 
        retention = OneDayInMs, 
        frequency = OneHourInMs)) { timer =>
      there was one (timer).scheduleAtFixedRate(any[TimerTask], meq(0L), meq(3600000L)) toResult
    }
  }
  
  def config(enabled: Boolean, retention: Long, frequency: Long): Config = {
    val config = mock[Config]
    config.getBoolean("trimming.enabled") returns enabled
    config.getMilliseconds("trimming.trimEventsOlderThan") returns retention
    config.getMilliseconds("trimming.frequency") returns frequency
    config
  }
  
  val ConfigWithTrimmingDisabled = config(false, OneDayInMs, OneHourInMs)
  
  def startTrimmerWithConfig(config: Config)(body: Timer => Result): Result = {
    val timer = mock[Timer]
    val trimmer = new Trimmer(timer)
    trimmer.start(config)
    body(timer)
  }
  
  val OneHourInMs = 60 * 60 * 1000
  val OneDayInMs = 24 * OneHourInMs
}