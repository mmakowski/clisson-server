package com.bimbr.clisson.server.database.h2

import com.bimbr.clisson.protocol._
import com.bimbr.clisson.protocol.Types._

/**
 * Types of events as stored in the database.
 * 
 * @author mmakowski
 * @since 1.0.0
 */
private[h2] object EventTypes {
  // constants for storage in the database
  val Checkpoint = 0: Byte
  val Split      = 1: Byte
  val Join       = 2: Byte
  
  /**
   * @return the name of event type corresponding to supplied type code
   */
  def typeName(typeCode: Byte) = typeCode match {
    case Checkpoint => id(classOf[Checkpoint])
    case Split      => "TODO"
    case Join       => "TODO"
  }
}