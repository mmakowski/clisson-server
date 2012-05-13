package integration

import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIUtils

import com.bimbr.clisson.server.config.Config

import com.bimbr.clisson.server.{ ServerApplication, SockoApplication }

trait IntegrationTest {
  def serverConfig: String
  def run(server: ServerApplication): Unit
  
  def main(args: Array[String]) = {
    System.setProperty("config", serverConfig)
    deleteDatabase()
    val server = startServer(args)
    run(server)
    stopServer(server) 
  }
  
  // assumes usage of H2 database
  def deleteDatabase(): Unit = {
    val dbBase = Config.fromPropertiesFile(serverConfig) match {
      case Left(msg)  => sys.error(msg) 
      case Right(cfg) => cfg("clisson.db.path").get
    }
    deleteIfExists(dbBase + ".h2.db")
    deleteIfExists(dbBase + ".trace.db")
  }
  
  def startServer(args: Array[String]): ServerApplication = {
    val server = SockoApplication
    server.start()
    Thread.sleep(5000)
    server
  }
  
  def stopServer(server: ServerApplication): Unit = server.stop()

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