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
    System.setProperty("config", serverConfig)
    val server = new SockoApplication()
  
  def withServer(test: => Result): Result = {
    deleteDatabase()
    startServer()
    val result = test
    stopServer()
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
  
  def startServer() = {
    server.start()
    waitUntilServerStarted()
  }
  
  def stopServer(): Unit = server.stop()

  def waitUntilServerStarted(): Unit = 
    // TODO: poll
    Thread.sleep(5000)
  
  def deleteIfExists(path: String): Unit = {
    val file = new java.io.File(path)
    if (file.exists) {
      if (!file.delete()) sys.error("unable to delete " + file)
    }
  }
  
  def trail(msgId: String) = responseTo("/trail/" + msgId)
  
  def responseTo(path: String, query: Option[String] = None) = {
    val client = new DefaultHttpClient
    val request = new HttpGet(URIUtils.createURI("http", "localhost", 9000, path, query.getOrElse(""), null))
    client.execute(request)
  }
  
}