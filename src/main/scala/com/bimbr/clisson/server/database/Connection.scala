package com.bimbr.clisson.server.database

import com.bimbr.clisson.protocol._ 

/**
 * Interface for objects that create database connections.
 */
trait Connector[C <: Connection] {
  def connect(): C
}

/**
 * A connection that abstracts out the interaction with concerete database.
 * 
 * @author mmakowski
 * @since 1.0.0
 */
trait Connection {
  def insertCheckpoint(checkpoint: Checkpoint): Unit
}

