package com.bimbr.time

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit._

/**
 * Parser for time units.
 * 
 * @since 1.0.0
 * @author mmakowski
 */
object TimeUnitParser {
  def parse(str: String): Option[TimeUnit] = str.toLowerCase.trim match {
    case "s"       => Some(SECONDS)
    case "second"  => Some(SECONDS)
    case "seconds" => Some(SECONDS)
    case "minute"  => Some(MINUTES)
    case "minutes" => Some(MINUTES)
    case "h"       => Some(HOURS)
    case "hour"    => Some(HOURS)
    case "hours"   => Some(HOURS)
    case "d"       => Some(DAYS)
    case "day"     => Some(DAYS)
    case "days"    => Some(DAYS)
    case _         => None 
  }
}
