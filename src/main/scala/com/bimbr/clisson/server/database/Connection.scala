package com.bimbr.clisson.server.database

import com.bimbr.clisson.protocol._ 

/**
 * Interface for objects that create database connections.
 */
trait Connector {
  def connect(): Connection
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

