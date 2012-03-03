package com.bimbr.clisson.server.database

import com.bimbr.clisson.protocol.StandaloneObject

/**
 * Commands that should be handled by database actors.
 * 
 * @author mmakowski
 * @since 1.0.0
 */
sealed trait Command
case class Insert[T <: StandaloneObject](obj: T) extends Command
case class GetTrail(messageId: String) extends Command
