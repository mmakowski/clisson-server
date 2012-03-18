package com.bimbr.clisson.server.database

import com.bimbr.clisson.protocol._ 

/**
 * Interface for objects that create database connections.
 */
trait Connector {
  /**
   * @return Left(errorMessage) if connection was unsuccesful; Right(connection) otherwise
   * @since 1.0.0
   */
  def connect(): Either[String, Connection]
}

/**
 * A connection that abstracts out the interaction with concerete database.
 * 
 * @author mmakowski
 * @since 1.0.0
 */
trait Connection {
  def getTrail(messageId: String): Option[Trail]
  def insertEvent(event: Event): Unit
}

