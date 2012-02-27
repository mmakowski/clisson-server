package com.bimbr.clisson.server

import com.bimbr.clisson.protocol.Json.fromJson

/**
 * Deserialises protocol objects.
 * 
 * @author mmakowski
 * @since 1.0.0
 */
class Deserialiser {
  def deserialise[T](cls: Class[T], json: String): Either[T, Exception] = {
    try {
      Left(fromJson(json, cls))
    } catch {
      case e: Exception => Right(e)
    }
  }
}