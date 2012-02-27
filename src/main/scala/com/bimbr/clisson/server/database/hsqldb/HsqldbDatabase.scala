package com.bimbr.clisson.server.database.hsqldb

import akka.actor.Actor
import com.bimbr.clisson.protocol._
import com.bimbr.clisson.server.database._

/**
 * A database actor that uses HSQLDB as the database backend.
 * 
 * @author mmakowski
 * @since 1.0.0
 */
class HsqldbDatabase extends Actor {
  def receive = {
    case Insert(obj) => insert(obj)
  }
  
  private def insert(obj: StandaloneObject) = obj match {
    case checkpoint: Checkpoint => println("TODO: insert checkpoint " + checkpoint)
    case _                      => println("TODO: log error")
  }
}