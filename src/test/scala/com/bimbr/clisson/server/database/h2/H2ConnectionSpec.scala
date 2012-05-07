package com.bimbr.clisson.server.database.h2

import scala.collection.JavaConverters._
import scala.collection.mutable.{ Set => MSet }

import java.sql.{ Connection, DriverManager, PreparedStatement }
import java.util.Date
import org.junit.runner.RunWith
import org.specs2.execute.Result
import org.specs2.matcher.Matcher
import org.specs2.mutable.{ After, Specification }
import org.specs2.runner.JUnitRunner
import com.bimbr.clisson.protocol.{ Event, Trail }


@RunWith(classOf[JUnitRunner])
class H2ConnectionSpec extends Specification {
  Class forName "org.h2.Driver"
  
  "H2Connection construction" should {
    "initialise database if database file does not exist" in withDb { (jdbcConn, h2Conn) => 
      val schemaVersion = "select value from metadata where key = 'schema.version'".stringResult(jdbcConn)
      schemaVersion must beSome.like { case str => str mustEqual SQL.SchemaVersion }
    }
    "upgrade database to current schema if database schema is out of date" in {
      todo
    }
  }
  "H2Connection" should {
    "store inserted events" in withDb { (_, h2Conn) =>
      val evt = event()
      h2Conn.insertEvent(evt)
      h2Conn.getTrail("msg1") must beSome.like { case trail => trail must haveTimeline (Seq(evt)) } 
    }
    "trim all events before specified cut-off time" in withDb { (_, h2Conn) =>
      val cutOffTime = new Date(3L)
      val eventsToInsert = Seq(
        event(timestamp = new Date(1L)),
        event(timestamp = new Date(2L)),
        event(timestamp = cutOffTime),
        event(timestamp = new Date(4L)),
        event(timestamp = new Date(5L))
      )
      val expectedTimelineAfterTrim = eventsToInsert.slice(2, 5)
      eventsToInsert foreach { h2Conn.insertEvent(_) }
      val trimmedEventsCount = h2Conn.trimEventsBefore(cutOffTime)
      (trimmedEventsCount mustEqual 2) and (h2Conn.getTrail("msg1") must beSome.like { case trail => trail must haveTimeline (expectedTimelineAfterTrim)})
    }
    "delete ids of messages whose all events have been trimmed" in withDb { (jdbcConn, h2Conn) =>
      h2Conn.insertEvent(event(timestamp = new Date(1L), inputMsgIds = MSet("msg2"), outputMsgIds = MSet("msg2")))
      h2Conn.trimEventsBefore(new Date(2L))
      val trimmedId = "select external_id from message_ids where external_id = 'msg2'".stringResult(jdbcConn)
      trimmedId must beNone
    }
  }
  
  def withDb(body: (Connection, H2Connection) => Result): Result = {
    val dbPath = System.getProperty("java.io.tmpdir") + "/" + getClass.getSimpleName + "-" + uniqueId()
    deleteDb(dbPath)
    val jdbcConn = DriverManager.getConnection("jdbc:h2:" + dbPath, "sa", "")
    try {
      val h2Conn = new H2Connection(jdbcConn)
      body(jdbcConn, h2Conn)
    } finally {
      jdbcConn.close()
      deleteDb(dbPath)
    }
  }
  
  var id = 0
  def uniqueId() = synchronized { 
    id += 1
    id - 1
  }
  
  def deleteDb(dbBase: String) = {
    deleteIfExists(dbBase + ".h2.db")
    deleteIfExists(dbBase + ".trace.db")
  }

  def deleteIfExists(path: String): Unit = {
    val file = new java.io.File(path)
    if (file.exists) {
      if (!file.delete()) sys.error("unable to delete " + file)
    }
  }
  
  def haveTimeline(timeline: Seq[Event]): Matcher[Trail] = (t: Trail) => (t.getTimeline.asScala == timeline, "the timeline of " + t + " is not " + timeline)
  
  val MsgIds = MSet("msg1")
  def event(timestamp: Date = new Date, inputMsgIds: MSet[String] = MsgIds, outputMsgIds: MSet[String] = MsgIds) = 
    new Event("h2 test", timestamp, inputMsgIds.asJava, outputMsgIds.asJava, "test event")
  
  class SqlStatement(sql: String) {
    def stringResult(jdbcConn: Connection): Option[String] = {
      val select = jdbcConn.prepareStatement(sql)
      val result = select.executeQuery()
      if (result.next) Some(result.getString(1)) else None
    }
  }
  
  implicit def stringToSqlStatement(sql: String): SqlStatement = new SqlStatement(sql)
}