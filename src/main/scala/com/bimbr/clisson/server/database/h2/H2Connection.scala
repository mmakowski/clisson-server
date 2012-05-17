package com.bimbr.clisson.server.database.h2

import java.lang.{ Long => JLong }
import java.sql.{ DriverManager, PreparedStatement, Timestamp }
import java.util.{ Date, Set => JSet }
import scala.collection.JavaConverters._
import scala.collection.mutable.{ Map => MMap, Set => MSet }
import scala.util.control.Exception._
import akka.actor.Actor
import com.bimbr.clisson.protocol._
import com.bimbr.clisson.server.database._
import com.bimbr.clisson.server.config.Config
import org.slf4j.LoggerFactory

/**
 * A connector for H2 database.
 * 
 * @author mmakowski
 * @since 1.0.0
 */
class H2Connector(config: Config) extends Connector {
  private val DbPathProperty = "clisson.db.path"
  private val Log = LoggerFactory.getLogger(classOf[H2Connector])

  Class forName ("org.h2.Driver")
  
  def connect() = config(DbPathProperty).toRight(DbPathProperty + " is not defined in " + config).fold(Left(_), p => Right(newH2Connection(p)))
  
  private def newH2Connection(dbPath: String): H2Connection = {
    Log.info("creating H2 connection to database at " + dbPath)
    new H2Connection(DriverManager.getConnection("jdbc:h2:" + dbPath, "sa", ""))
  }
}

// TODO: a lot of cleanup required: closing statements, style
private[h2] class H2Connection(val conn: java.sql.Connection) extends Connection {
  import SQL._
  import MessageRoles._

  private val Log = LoggerFactory.getLogger(classOf[H2Connection])
  
  if (!isInitialised) initialise()
  // TODO: upgrade schema if required

  def close() = conn.close()
  
  def getAverageLatency(startTimeOpt: Option[Date], endTimeOpt: Option[Date]) = catching(classOf[Exception]) either {
    val startTime = new Timestamp(startTimeOpt.map(_.getTime).getOrElse(0L))
    val endTime = new Timestamp(endTimeOpt.map(_.getTime).getOrElse(Long.MaxValue))
    Log.debug("calculating average latency for period " + startTime + " to " + endTime)
    
    val select = conn prepareStatement SelectAverageComponentLatencies
    select setTimestamp (1, startTime)
    select setTimestamp (2, endTime)
    select setTimestamp (3, startTime)
    select setTimestamp (4, endTime)

    val result = select.executeQuery()

    var endToEndLatency: Long = -1
    val componentLatencies = MMap[String, Long]()
    while (result.next) {
      val componentId = result getString 1
      val latency     = result getLong   2
      if (componentId == "__etoe__") endToEndLatency = latency
      else componentLatencies put (componentId, latency)
    }
    AverageLatency(endToEndLatency, componentLatencies.asJava)
  }
  
  def getThroughput(startTimeD: Date, endTimeD: Date) = catching(classOf[Exception]) either {
    val startTime = new Timestamp(startTimeD.getTime)
    val endTime = new Timestamp(endTimeD.getTime)
    Log.debug("calculating throughput for period " + startTime + " to " + endTime)
    
    val select = conn prepareStatement SelectComponentThroughputs
    select setTimestamp (1, startTime)
    select setTimestamp (2, endTime)
    select setTimestamp (3, startTime)
    select setTimestamp (4, endTime)
    select setTimestamp (5, startTime)
    select setTimestamp (6, endTime)

    val result = select.executeQuery()

    var endToEndThroughput: Double = -1
    val componentThroughputs = MMap[String, Double]()
    while (result.next) {
      val componentId = result getString 1
      val latency     = result getDouble 2
      if (componentId == "__etoe__") endToEndThroughput = latency
      else componentThroughputs put (componentId, latency)
    }
    Throughput(endToEndThroughput, componentThroughputs.asJava)
  }
  
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
            val event = new Event(source, timestamp, inputMsgIds.asJava, outputMsgIds.asJava, n2e(description))
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
        val event = new Event(source, timestamp, inputMsgIds.asJava, outputMsgIds.asJava, n2e(description))
        events += ((eventId, event))
      case None => // do nothing
    }
    
    if (events isEmpty) None 
    else Some(new Trail(events.asJava, eventGraph.asJava, initialEventIds.asJava))
  }
  
  def insertEvent(event: Event) = {
    val inputMsgIds = event.getInputMessageIds.asScala.map(getOrInsertMessageId)
    val outputMsgIds = event.getOutputMessageIds.asScala.map(getOrInsertMessageId)
    val eventId = insertEvent(event.getSourceId, event.getTimestamp, event.getDescription)
    insertEventMessages(eventId, inputMsgIds, SourceMsg)
    insertEventMessages(eventId, outputMsgIds, ResultMsg)
    conn.commit()
  }

  def trimEventsBefore(cutOffTime: Date) = {
    val eventDelete = conn prepareStatement DeleteEventsBefore
    eventDelete setTimestamp (1, new Timestamp(cutOffTime.getTime))
    val deletedEventsCount = eventDelete.executeUpdate()
    val messageIdDelete = conn prepareStatement DeleteOrphanedMessageIds
    messageIdDelete.executeUpdate()
    conn.commit()
    deletedEventsCount
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
    Log.info("running database initialisation DDL...")
    execute(InitDdl) 
    Log.info("running database initialisation DML...")
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