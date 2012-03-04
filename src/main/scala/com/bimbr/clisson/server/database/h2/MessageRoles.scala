package com.bimbr.clisson.server.database.h2

/**
 * Message roles stored in the database.
 * 
 * @author mmakowski
 * @since 1.0.0
 */
private[h2] object MessageRoles {
  val CheckpointMsg = 0: Byte
  val SourceMsg     = 1: Byte
  val ResultMsg     = 2: Byte
}