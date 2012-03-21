package integration

import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIUtils

import com.bimbr.clisson.server.config.Config


trait IntegrationTest {
  def serverConfig: String
  def run(server: Thread): Unit
  
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
  
  // TODO: separate JVM would be more realistic
  def startServer(args: Array[String]): Thread = {
    val server = new Thread(new Runnable() {
      def run() = com.bimbr.clisson.server.ClissonServerApp.main(args)
    })
    server.start()
    Thread.sleep(5000)
    server
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
  
  def trail(msgId: String) = {
    val client = new DefaultHttpClient
    val request = new HttpGet(URIUtils.createURI("http", "localhost", 9000, "/trail/" + msgId, "", null))
    client.execute(request)
  }
}