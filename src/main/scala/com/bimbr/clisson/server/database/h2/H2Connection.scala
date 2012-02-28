package com.bimbr.clisson.server.database.h2

import akka.actor.Actor
import java.sql.DriverManager

import com.bimbr.clisson.protocol._
import com.bimbr.clisson.server.database._

/**
 * A connector for H2 database.
 * 
 * @author mmakowski
 * @since 1.0.0
 */
class H2Connector extends Connector {
  Class forName ("org.h2.Driver")
  // TODO: read from properties
  def connect = new H2Connection(DriverManager.getConnection("jdbc:h2:~/scratch/clisson-db", "sa", ""))
}

private[h2] class H2Connection(val conn: java.sql.Connection) extends Connection {
  def insertCheckpoint(checkpoint: Checkpoint) = println("TODO: insert checkpoint " + checkpoint)
}