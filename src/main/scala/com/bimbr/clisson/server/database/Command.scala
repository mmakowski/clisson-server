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
 * @since 1.0.0
 */
case class GetTrail(messageId: String) extends Command
/**
 * @since 1.0.0
 */
case class TrimEventsBefore(cutOffTime: Date) extends Command
