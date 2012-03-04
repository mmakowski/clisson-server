package com.bimbr.clisson.server.database.h2

import java.lang.{ Long => JLong }
import java.sql.{ DriverManager, PreparedStatement }
import java.util.{ Date, Set => JSet }
import scala.collection.JavaConversions._
import scala.collection.mutable.{ Map => MMap, Set => MSet }

import akka.actor.Actor

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
  import SQL._
  import MessageRoles._
  
  if (!isInitialised) initialise()
  // TODO: upgrade schema if required

  def getTrail(externalMessageId: String) = {
    val select = conn prepareStatement SelectEventsForExternalId
    select setString (1, externalMessageId)
    val result = select.executeQuery()
    val events = MMap[JLong, Event]()
    val eventGraph = MMap[JLong, JSet[JLong]]()
    val initialEventIds = MSet[JLong]()
    while (result.next) {
      val eventId     = result getLong      1
      val source      = result getString    2
      val timestamp   = result getTimestamp 3
      val priority    = result getInt       4
      val typeId      = result getByte      5
      val description = result getString    6
      val messageId   = result getString    7
      val messageRole = result getByte      8
      
      if (initialEventIds isEmpty) initialEventIds += eventId // naive for now
      val event = eventFrom(source, timestamp, priority, typeId, if (description == null) "" else description, messageId, messageRole)
      events += ((eventId, event))
      // TODO: event graph
    }
    
    if (events isEmpty) None 
    else Some(new Trail(events, eventGraph, initialEventIds))
  }
  
  def insertCheckpoint(checkpoint: Checkpoint) = {
    val msgId = getOrInsertMessageId(checkpoint getMessageId)
    val eventId = insertEvent(checkpoint.getEventHeader, EventTypes.Checkpoint, checkpoint.getDescription)
    insertEventMessage(eventId, msgId, CheckpointMsg)
    conn.commit()
  }

  private def eventFrom(source:      String,
                        timestamp:   Date,
                        priority:    Int,
                        typeId:      Byte,
                        description: String,
                        messageId:   String,
                        messageRole: Byte) = {
    val header = new EventHeader(source, timestamp, priority)
    typeId match {
      case EventTypes.Checkpoint => new Checkpoint(header, messageId, description)
      case _                     => throw new IllegalStateException("unsupported event type: " + typeId)
    }
  }
  
  private def getOrInsertMessageId(externalId: String) = {
    val select = conn prepareStatement SelectMessageId 
    select setString (1, externalId)
    val result = select.executeQuery()
    if (result.next) result getLong 1 else insertMessageId(externalId)
  }
  
  private def insertMessageId(externalId: String) = {
    val msgId = getNextSequenceValue(SelectNextMessageId)
    
    val msgInsert = conn prepareStatement InsertMessageId
    msgInsert setString (1, externalId)
    msgInsert setLong   (2, msgId)
    msgInsert.execute()
    
    msgId
  }

  private def insertEvent(header: EventHeader, typeId: Byte, description: String) = {
    val eventId = getNextSequenceValue(SelectNextEventId)
    
    val eventInsert = conn prepareStatement InsertEvent
    eventInsert setLong      (1, eventId)
    eventInsert setString    (2, header.getSourceId)
    eventInsert setTimestamp (3, new java.sql.Timestamp(header.getTimestamp.getTime))
    eventInsert setInt       (4, header.getPriority)
    eventInsert setByte      (5, typeId)
    eventInsert setString    (6, description)
    eventInsert.execute()
    
    eventId
  }
  
  private def insertEventMessage(eventId: Long, messageId: Long, role: Byte) = {
    val eventMessageInsert = conn prepareStatement InsertEventMessage
    eventMessageInsert setLong (1, eventId)
    eventMessageInsert setLong (2, messageId)
    eventMessageInsert.setByte (3, role)
    eventMessageInsert.execute()
  }
  
  private def getNextSequenceValue(sql: String) = {
    val nextValueSelect = conn prepareStatement SelectNextMessageId
    val result = nextValueSelect.executeQuery()
    if (!result.next) throw new IllegalStateException("no value returned by query: " + sql)
    result getLong 1
  }
  
  private def isInitialised = try {    
    execute(InitialisedCheck)
    true
  } catch {
    case e: org.h2.jdbc.JdbcSQLException => false
  }
  
  private def initialise() = {
    // TODO: log instead of println
    println("running database initialisation DDL...")
    execute(InitDdl) 
    println("running database initialisation DML...")
    execute(InitDml)
  }
  
  private def execute(sql: String) = {
    val st = conn prepareStatement sql 
    st execute()
  }
}