package framework

import org.specs2.Specification
import org.specs2.execute.Result

import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIUtils

import com.bimbr.clisson.server.config.Config

import com.bimbr.clisson.server.{ ServerApplication, SockoApplication }


trait ClissonServerSpecification extends Specification {
  val serverConfig = "tests/" + getClass.getSimpleName + "-server.properties"
  
  def withServer(test: => Result): Result = {
    deleteDatabase()
    val server = startServer()
    val result = test
    stopServer(server)
    result
  }
  
  // assumes usage of H2 database
  private def deleteDatabase(): Unit = {
    val dbBase = Config.fromPropertiesFile(serverConfig) match {
      case Left(msg)  => sys.error(msg) 
      case Right(cfg) => cfg("clisson.db.path").get
    }
    deleteIfExists(dbBase + ".h2.db")
    deleteIfExists(dbBase + ".trace.db")
  }
  
  private def startServer(): ServerApplication = {
    val origConfig = System.getProperty("config")
    System.setProperty("config", serverConfig)
    val server = SockoApplication
    server.start()
    waitUntilServerStarted()
    //System.setProperty("config", origConfig)
    server
  }
  
  def stopServer(server: ServerApplication): Unit = server.stop()

  def waitUntilServerStarted(): Unit = 
    // TODO: poll
    Thread.sleep(5000)
  
  def deleteIfExists(path: String): Unit = {
    val file = new java.io.File(path)
    if (file.exists) {
      if (!file.delete()) sys.error("unable to delete " + file)
    }
  }
  
  def trail(msgId: String) = {
    val client = new DefaultHttpClient
    val request = new HttpGet(URIUtils.createURI("http", "localhost", 9000, "/trail/" + msgId, "", null))
    client.execute(request)
  }
}