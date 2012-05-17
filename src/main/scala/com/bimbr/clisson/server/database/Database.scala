package com.bimbr.clisson.server.database

import com.bimbr.clisson.protocol._

import akka.actor.Actor

import org.slf4j.LoggerFactory


/**
 * An actor that handles interaction with the database.
 * 
 * @author mmakowski
 * @since 1.0.0
 */
class Database(val connector: Connector) extends Actor {
  private val Log = LoggerFactory.getLogger(classOf[Database])
  
  private val conn = connector.connect() match {
    case Left(errorMessage) => throw new RuntimeException(errorMessage)
    case Right(conn)        => conn
  }
  
  def receive = {
    case Insert(obj)                   => insert(obj)
    case GetAverageLatency(start, end) => sender ! (conn getAverageLatency (start, end))
    case GetThroughput(start, end)     => sender ! (conn getThroughput (start, end))
    case GetTrail(msgId)               => sender ! (conn getTrail msgId)
    case TrimEventsBefore(cutOffTime)  => sender ! (conn trimEventsBefore cutOffTime)
  }
  
  private def insert(obj: StandaloneObject) = obj match {
    case event: Event => conn insertEvent event
    case _            => Log.error("unsupported type of object to insert: " + obj)
  }
  
  override def postStop(): Unit = conn.close()
}