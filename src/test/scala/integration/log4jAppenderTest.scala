package integration

import com.bimbr.clisson.server.config.Config
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIUtils
import org.apache.http.util.EntityUtils

object log4jAppenderTest extends IntegrationTest {
  val serverConfig = "integration/log4jAppender/clisson-server.properties"
  System.setProperty("config", serverConfig)
    
  def run(server: Thread) = {
    loggingApp.run()
    Thread.sleep(1000)
    checkTrail()
  }
  
  def checkTrail() = {
    val client = new DefaultHttpClient
    val request = new HttpGet(URIUtils.createURI("http", "localhost", 9000, "/trail/msg-1", "", null))
    val response = client.execute(request)
    println("response to " + request.getURI + ": " + response + "\n" + EntityUtils.toString(response.getEntity))
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