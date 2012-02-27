package com.bimbr.clisson.server.database

import com.bimbr.clisson.protocol._

import akka.actor.Actor

/**
 * An actor that handles interaction with the database.
 * 
 * @author mmakowski
 * @since 1.0.0
 */
class Database[C <: Connection](val connector: Connector[C]) extends Actor {
  private val conn = connector.connect()
  
  def receive = {
    case Insert(obj) => insert(obj)
  }
  
  private def insert(obj: StandaloneObject) = obj match {
    case checkpoint: Checkpoint => println("TODO: insert checkpoint " + checkpoint)
    case _                      => println("TODO: log error")
  }
}