package com.bimbr.clisson.server.database.h2

/**
 * Message roles stored in the database.
 * 
 * @author mmakowski
 * @since 1.0.0
 */
private[h2] object MessageRoles {
  val SourceMsg     = 0: Byte
  val ResultMsg     = 1: Byte
}