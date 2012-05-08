package com.bimbr.time

import java.util.concurrent.TimeUnit
import scala.util.control.Exception._

/**
 * Duration of time.
 * 
 * @since 1.0.0
 * @author mmakowski
 */
case class Duration(count: Int, unit: TimeUnit) {
  def inMillis: Long = unit toMillis count
}

/**
 * Companio object for Durations.
 * 
 * @since 1.0.0
 * @author mmakowski
 */
object Duration {
  val DurationRegex = """(\d+)\s*(\w+)""".r
  
  /**
   * Parses string into Duration The string format should be 
   * 
   * <count>[ ]<unit>
   * 
   * where <count> is a number and <unit> is one of the units supported by TimeUnit
   */
  def parse(str: String): Option[Duration] = str match {
    case DurationRegex(countStr, unitStr) => for (c <- parseCount(countStr); u <- parseUnit(unitStr)) yield Duration(c, u)
    case _                          => None
  }
  
  private def parseCount(str: String): Option[Int] = allCatch.opt(Integer.parseInt(str))
  private def parseUnit(str: String): Option[TimeUnit] = TimeUnitParser.parse(str)
}