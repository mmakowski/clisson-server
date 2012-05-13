package com.bimbr.clisson.server.database

import java.util.Date
import com.bimbr.clisson.protocol.StandaloneObject

/**
 * Commands that should be handled by database actors.
 * 
 * @author mmakowski
 * @since 1.0.0
 */
sealed trait Command
/**
 * @since 1.0.0
 */
case class Insert[T <: StandaloneObject](obj: T) extends Command
/**
 * @param startTime the start of time window for metric calculation; if None, then start at the beginning of the history
 * @param endTime the end of time window for metric calculation; if None, then end at the end of the history 
 * @since 1.0.0
 */
case class GetAverageLatency(startTime: Option[Date], endTime: Option[Date]) extends Command
/**
 * @param startTime the start of time window for metric calculation
 * @param endTime the end of time window for metric calculation 
 * @since 1.0.0
 */
case class GetThroughput(startTime: Date, endTime: Date) extends Command
/**
 * @since 1.0.0
 */
case class GetTrail(messageId: String) extends Command
/**
 * @since 1.0.0
 */
case class TrimEventsBefore(cutOffTime: Date) extends Command
