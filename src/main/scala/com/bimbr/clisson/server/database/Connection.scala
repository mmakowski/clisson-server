package com.bimbr.clisson.server.database

import java.util.Date
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
  /**
   * @return the trail of specified message (if exists)
   */
  def getTrail(messageId: String): Option[Trail]
  /**
   * inserts event to the database
   */
  def insertEvent(event: Event): Unit
  /**
   * deletes all events with timestamp before cutOffTime
   * @return the number of events trimmed
   */
  def trimEventsBefore(cutOffTime: Date): Int
}

