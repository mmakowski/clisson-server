package com.bimbr.clisson.server

import java.util.{ Timer, TimerTask }
import scala.collection.JavaConverters._
import com.typesafe.config.{ Config, ConfigFactory }
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
    case Right((retentionMs, frequencyMs)) => ()
      timer.scheduleAtFixedRate(trimEvents(retentionMs), 0, frequencyMs)
  }
  
  def stop() = {
    timer.cancel()
    Log.info("cancelled database trimming")
  }
  
  private def trimEvents(retentionMs: Long): TimerTask = new TimerTask {
    def run(): Unit = {
      Log.info("TODO: trim!")
    }
  } 
  
  private def parseConfig(config: Config): Either[String, (Long, Long)] = {
    val configWithDefaults = config.withFallback(ConfigFactory.parseMap(Map(
        TrimmingEnabled     -> "false",
        TrimEventsOlderThan -> "1 year",
        TrimmingFrequency   -> "1 day"
        ).asJava))
    val frequencyMs = config.getMilliseconds(TrimmingFrequency) 
    val retentionMs = config.getMilliseconds(TrimEventsOlderThan)
    val enabled = config.getBoolean(TrimmingEnabled)
    if (!enabled) Left("trimming is disabled") else Right((retentionMs, frequencyMs))
    
  }
  
  private def parseDuration(config: Config, key: String, defaultValue: String): Either[String, Duration] = {
    val durationStr = getWithDefault(config, key, defaultValue) 
    Duration.parse(durationStr).toRight("unable to parse " + key + " value " + durationStr)
  }
  
  private def getWithDefault(config: Config, key: String, defaultValue: String): String = Option(config.getString(key)) getOrElse {
    Log.debug(key + " is not specified in the config, using default: " + defaultValue)
    defaultValue
  }
  
}