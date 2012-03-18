package com.bimbr.clisson.server.database.h2

import java.lang.{ Long => JLong }
import java.sql.{ DriverManager, PreparedStatement }
import java.util.{ Date, Set => JSet }
import scala.collection.JavaConversions._
import scala.collection.mutable.{ Map => MMap, Set => MSet }

import akka.actor.Actor

import scalaz._

import com.bimbr.clisson.protocol._
import com.bimbr.clisson.server.database._
import com.bimbr.clisson.server.config.Config



/**
 * A connector for H2 database.
 * 
 * @author mmakowski
 * @since 1.0.0
 */
class H2Connector(config: Config) extends Connector {
  import Scalaz._
  private val DbPathProperty = "clisson.db.path"
  Class forName ("org.h2.Driver")
  
  def connect() = (for {
    dbPath <- validation(config(DbPathProperty).toRight(DbPathProperty + " is not defined in " + config))
  } yield new H2Connection(DriverManager.getConnection("jdbc:h2:" + dbPath, "sa", ""))).either
}

// TODO: a lot of cleanup required: closing statements, style
private[h2] class H2Connection(val conn: java.sql.Connection) extends Connection {
  import SQL._
  import MessageRoles._
  
  if (!isInitialised) initialise()
  // TODO: upgrade schema if required

  // TODO: very nasty, refactor
  def getTrail(externalMessageId: String) = {
    val select = conn prepareStatement SelectEventsForExternalId
    select setString (1, externalMessageId)
    val result = select.executeQuery()
    val events = MMap[JLong, Event]()
    val eventGraph = MMap[JLong, JSet[JLong]]()
    val initialEventIds = MSet[JLong]()
    
    var prevEventId = -1L // none
    var inputMsgIds = MSet[String]()
    var outputMsgIds = MSet[String]()
    var lastEventParams = None: Option[(Long, String, Date, String, String, Byte)]
    while (result.next) {
      val eventId     = result getLong      1
      val source      = result getString    2
      val timestamp   = result getTimestamp 3
      val description = result getString    4
      val messageId   = result getString    5
      val messageRole = result getByte      6
      
      if (prevEventId == -1L) prevEventId = eventId
      if (initialEventIds isEmpty) initialEventIds += eventId // naive for now
      if (prevEventId != eventId) {
        lastEventParams match {
          case Some((eventId, source, timestamp, description, messageId, messageRole)) =>
            val event = new Event(source, timestamp, inputMsgIds, outputMsgIds, n2e(description))
            events += ((prevEventId, event))
          case None => throw new IllegalStateException("impossible: no lastEventParams available")
        }
        inputMsgIds = MSet[String]()
        outputMsgIds = MSet[String]()
        prevEventId = eventId
      }
      
      messageRole match {
        case SourceMsg => inputMsgIds += messageId
        case ResultMsg => outputMsgIds += messageId
      }
      // TODO: event graph
      lastEventParams = Some((eventId, source, timestamp, description, messageId, messageRole))
    }
    result.close()
    
    lastEventParams match {
      case Some((eventId, source, timestamp, description, messageId, messageRole)) =>
        val event = new Event(source, timestamp, inputMsgIds, outputMsgIds, n2e(description))
        events += ((eventId, event))
      case None => // do nothing
    }
    
    if (events isEmpty) None 
    else Some(new Trail(events, eventGraph, initialEventIds))
  }
  
  def insertEvent(event: Event) = {
    val inputMsgIds = event.getInputMessageIds.map(getOrInsertMessageId)
    val outputMsgIds = event.getOutputMessageIds.map(getOrInsertMessageId)
    val eventId = insertEvent(event.getSourceId, event.getTimestamp, event.getDescription)
    insertEventMessages(eventId, inputMsgIds, SourceMsg)
    insertEventMessages(eventId, outputMsgIds, ResultMsg)
    conn.commit()
  }

  private def n2e(str: String) = if (str == null) "" else str
  
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
    msgInsert.close()
    
    msgId
  }

  private def insertEvent(sourceId:    String,
                          timestamp:   Date,
                          description: String) = {
    val eventId = getNextSequenceValue(SelectNextEventId)
    
    val eventInsert = conn prepareStatement InsertEvent
    eventInsert setLong      (1, eventId)
    eventInsert setString    (2, sourceId)
    eventInsert setTimestamp (3, new java.sql.Timestamp(timestamp.getTime))
    eventInsert setString    (4, description)
    eventInsert.execute()
    eventInsert.close()
    
    eventId
  }
  
  private def insertEventMessages(eventId: Long, messageIds: Traversable[Long], role: Byte) = {
    val eventMessageInsert = conn prepareStatement InsertEventMessage
    for (messageId <- messageIds) {
        eventMessageInsert setLong (1, eventId)
        eventMessageInsert setLong (2, messageId)
        eventMessageInsert setByte (3, role)
        eventMessageInsert addBatch()
    }
    eventMessageInsert.executeBatch()
    eventMessageInsert.close()
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
    try {
      st execute()
    } finally {
      st.close()
    }
  }
}