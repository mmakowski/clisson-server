package com.bimbr.time

import java.util.concurrent.TimeUnit._
import org.junit.runner.RunWith
import org.specs2.Specification
import org.specs2.matcher.DataTables
import org.specs2.runner.JUnitRunner


@RunWith(classOf[JUnitRunner])
class DurationSpec extends Specification with DataTables { def is =
  "durations should be parsed correctly" ! parseTable
  
  def parseTable = 
    "input"      || "count" || "unit"  |
    "3s"         !! 3       !! SECONDS |
    "1 second"   !! 1       !! SECONDS |
    "13 seconds" !! 13      !! SECONDS | 
    "1 minute"   !! 1       !! MINUTES |
    "13 minutes" !! 13      !! MINUTES |
    "3h"         !! 3       !! HOURS   |
    "1 hour"     !! 1       !! HOURS   |
    "13 hours"   !! 13      !! HOURS   |
    "3d"         !! 3       !! DAYS    |
    "1 day"      !! 1       !! DAYS    |
    "13 days"    !! 13      !! DAYS    |> {
    (in, count, unit) => Duration.parse(in) mustEqual Some(Duration(count, unit))    
  }
}