package com.bimbr.clisson.server.database.h2

import scala.collection.JavaConverters._
import scala.collection.mutable.{ Set => MSet }

import java.sql.{ Connection, DriverManager, PreparedStatement }
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
      val select = jdbcConn.prepareStatement("select value from metadata where key = 'schema.version'")
      val result = select.executeQuery()
      val selectedVersion = if (result.next) Some(result.getString(1)) else None
      selectedVersion must beSome.like { case str => str mustEqual SQL.SchemaVersion }
    }
    "upgrade database to current schema if database schema is out of date" in {
      todo
    }
  }
  "H2Connection" should {
    "store inserted events" in withDb { (_, h2Conn) =>
      val event = new Event("h2 test", new java.util.Date, MSet("msg1").asJava, MSet("msg2", "msg3").asJava, "test event")
      h2Conn.insertEvent(event)
      h2Conn.getTrail("msg1") must beSome.like { case trail => trail must haveEvents (Seq(event)) } 
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
  
  def haveEvents(timeline: Seq[Event]): Matcher[Trail] = (t: Trail) => (t.getTimeline.asScala == timeline, "the timeline of " + t + " is not " + timeline)
}