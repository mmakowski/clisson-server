package integration

import com.bimbr.clisson.server.config.Config
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIUtils
import org.apache.http.util.EntityUtils

object log4jAppenderTest {
  val ConfigFile = "integration/log4jAppender/clisson-server.properties"
  System.setProperty("config", ConfigFile)
    
  def main(args: Array[String]) = {
    deleteDatabase()
    val server = startServer(args)
    loggingApp.run()
    Thread.sleep(1000)
    checkTrail()
    stopServer(server) 
  }
  
  // assumes usage of H2 database
  def deleteDatabase(): Unit = {
    val dbBase = Config.fromPropertiesFile(ConfigFile) match {
      case Left(msg)  => sys.error(msg) 
      case Right(cfg) => cfg("clisson.db.path").get
    }
    deleteIfExists(dbBase + ".h2.db")
    deleteIfExists(dbBase + ".trace.db")
  }
  
  // TODO: separate JVM would be more realistic
  def startServer(args: Array[String]): Thread = {
    val server = new Thread(new Runnable() {
      def run() = com.bimbr.clisson.server.ClissonServerApp.main(args)
    })
    server.start()
    Thread.sleep(5000)
    server
  }

  def checkTrail() = {
    val client = new DefaultHttpClient
    val request = new HttpGet(URIUtils.createURI("http", "localhost", 9000, "/trail/msg-1", "", null))
    val response = client.execute(request)
    println("response to " + request.getURI + ": " + response + "\n" + EntityUtils.toString(response.getEntity))
  }
  
  def stopServer(server: Thread): Unit = {
    // server.stop() doesn't suffice, need more drastic solution
    System.exit(0)
  }
  
  def deleteIfExists(path: String): Unit = {
    val file = new java.io.File(path)
    if (file.exists) {
      if (!file.delete()) sys.error("unable to delete " + file)
    }
  }
  
  // the app that logs through log4j and whose events should end up with the server
  object loggingApp {
    import scala.collection.JavaConversions._
    import org.apache.log4j.{ Logger, PropertyConfigurator }
    import org.apache.log4j.spi.LoggingEvent
    import com.bimbr.clisson.client.log4j.EventTransformation
    import com.bimbr.clisson.protocol.Event
    
    System.setProperty("clisson.config", "classpath://integration/log4jAppender/clisson-client.properties")
    PropertyConfigurator.configure(getClass.getClassLoader.getResource("integration/log4jAppender/log4j.properties")) 
    val log = Logger.getLogger(getClass)
    
    def run() = {
      log.info("awaiting msg-1 (this line should not be sent)")
      log.info("msg-1: is being processed")
      log.warn("this should be ignored as well")
      log.error("msg-1: error happened!")
    }
    
    // the transformation from log4j Event to Clisson Event is a part of the client app
    class Transformation extends EventTransformation {
      def perform(log4jEvent: LoggingEvent): Event = {
        val logMessage = log4jEvent.getRenderedMessage
        val msgId = """(.+): .*""".r.findFirstMatchIn(logMessage) match {
          case None    => throw new EventTransformation.IgnoreEventException
          case Some(m) => m.group(1)
        }
        new Event("log4jAppenderTest", new java.util.Date, Set(msgId), Set(msgId), logMessage)
      }
    }
  }
  
}