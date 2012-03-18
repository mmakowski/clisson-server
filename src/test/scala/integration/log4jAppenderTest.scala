package integration

import com.bimbr.clisson.server.config.Config

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
    println("TODO: check trail")
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
    
    def run() = log.info("msg-1 is being processed")

    // the transformation from log4j Event to Clisson Event is a part of the client app
    class Transformation extends EventTransformation {
      def perform(log4jEvent: LoggingEvent): Event = {
        val logMessage = log4jEvent.getRenderedMessage
        val msgId = "dupa"
        new Event("log4jAppenderTest", new java.util.Date, Set(msgId), Set(msgId), logMessage)
      }
    }
  }
  
}