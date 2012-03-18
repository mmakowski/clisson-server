package integration

object log4jAppenderTest {
  def main(args: Array[String]) = {
    // TODO: delete database if exists
    val server = startServer(args)
    runLoggingApp()
    checkTrail()
    stopServer(server) 
  }
  
  // TODO: separate JVM would be more realistic
  def startServer(args: Array[String]) = {
    val server = new Thread(new Runnable() {
      def run() = {
        System.setProperty("config", "log4jAppenderTest-server.properties")
        com.bimbr.clisson.server.ClissonServerApp.main(args)
      }
    })
    server.start()
    Thread.sleep(5000)
    server
  }

  def runLoggingApp() = {
    println("TODO: run logging app")
  }
  
  def checkTrail() = {
    println("TODO: check trail")
  }
  
  def stopServer(server: Thread) = {
    // server.stop() doesn't suffice, need more drastic solution
    System.exit(0)
  }
}