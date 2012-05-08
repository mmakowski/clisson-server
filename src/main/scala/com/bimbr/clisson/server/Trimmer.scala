package com.bimbr.clisson.server

import java.util.{ Timer, TimerTask }
import com.bimbr.clisson.server.config.Config
import com.bimbr.time.{ Duration }
import org.slf4j.LoggerFactory

/**
 * Handles trimming of the events in the database.
 * 
 * @since 1.0.0
 * @author mmakowski
 */
class Trimmer(val timer: Timer) {
  private val TrimmingEnabled = "trimming.enabled"
  private val TrimEventsOlderThan = "trimming.trimEventsOlderThan"
  private val TrimmingFrequency = "trimming.frequency"
  private val Log = LoggerFactory.getLogger(classOf[Trimmer])
  
  def start(config: Config) = parseConfig(config) match {
    case Left(message)                 => Log.warn(message)
    case Right((retention, frequency)) => ()
      timer.scheduleAtFixedRate(trimEvents(retention), 0, frequency inMillis)
  }
  
  def stop() = {
    timer.cancel()
    Log.info("cancelled database trimming")
  }
  
  private def trimEvents(retention: Duration): TimerTask = new TimerTask {
    def run(): Unit = {
      Log.info("TODO: trim!")
    }
  } 
  
  private def parseConfig(config: Config): Either[String, (Duration, Duration)] = {
    val frequency = parseDuration(config, TrimmingFrequency, "1 day") 
    val retention = parseDuration(config, TrimEventsOlderThan, "365 days")
    val enabled = java.lang.Boolean.parseBoolean(getWithDefault(config, TrimmingEnabled, "false"))
    // TODO: this is nasty; use scalaz validation?
    if (!enabled) Left("trimming is disabled")
    else (frequency, retention) match {
      case (Right(f), Right(r)) => Right((r, f))
      case (Left(e1), Left(e2)) => Left(e1)
    }
    
  }
  
  private def parseDuration(config: Config, key: String, defaultValue: String): Either[String, Duration] = {
    val durationStr = getWithDefault(config, key, defaultValue) 
    Duration.parse(durationStr).toRight("unable to parse " + key + " value " + durationStr)
  }
  
  private def getWithDefault(config: Config, key: String, defaultValue: String): String = config(key) getOrElse {
    Log.debug(key + " is not specified in the config, using default: " + defaultValue)
    defaultValue
  }
  
}