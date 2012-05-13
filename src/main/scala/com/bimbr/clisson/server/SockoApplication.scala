package com.bimbr.clisson.server

import java.io.File
import java.net.URLDecoder.decode
import akka.pattern.ask
import akka.util.Timeout
import akka.util.duration._
import akka.actor.{ ActorSystem, Props, Actor, ActorRef }
import akka.dispatch.{ Await, Future }
import akka.routing.FromConfig
import com.bimbr.clisson.protocol.{ Event, Trail }
import com.bimbr.clisson.protocol.Json.fromJson
import com.bimbr.clisson.protocol.Types.classFor
import com.bimbr.clisson.server.database.{ Database, Insert, GetTrail }
import com.bimbr.clisson.server.config.Config
import com.bimbr.clisson.server.socko._
import org.jboss.netty.handler.codec.http.HttpResponseStatus.{ ACCEPTED, BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND }
import org.mashupbots.socko.context.HttpRequestProcessingContext
import org.mashupbots.socko.routes.{ GET, HttpRequest, POST, Path, PathSegments, Routes }
import org.mashupbots.socko.webserver.{ WebServer, WebServerConfig }
import org.slf4j.LoggerFactory
import org.mashupbots.socko.processors.StaticFileProcessor
import org.mashupbots.socko.processors.StaticFileRequest

/**
 * A server application based on socko
 * 
 * @since 1.0.0
 * @author mmakowski
 */
object SockoApplication extends ServerApplication {
  // get a logger to initialise logging system before other components require it to avoid http://www.slf4j.org/codes.html#substituteLogger
  LoggerFactory.getLogger("SockoApplication")
  private var isRunning = false

  private lazy val config = loadConfig()
  private lazy val webServerConfig = WebServerConfig(
    serverName = "ClissonWebServer",
    port       = Integer.parseInt(config("http.port").getOrElse("9000"))
  )
  
  private val          StaticFileDir = new File("static")
  private val          system = ActorSystem("clisson-server");
  private implicit val timeout = Timeout(10000 milliseconds)
  private val          database = system.actorOf(databaseActorType, name = "database")
  private val          staticFileProcessor = system.actorOf(Props[StaticFileProcessor].
                                                            withRouter(FromConfig()).
                                                            withDispatcher("pinned-dispatcher"), name = "static-file-router")
  
  /**
   * Defines how HTTP requests to different paths are handled.
   */
  val routes = Routes({
    case HttpRequest(httpRequest) => httpRequest match {
      case GET  (Path("/favicon.ico"))                       => staticFileProcessor ! getStaticFile(httpRequest, "favicon.ico") 
      case GET  (PathSegments("metric" :: metricId :: rest)) => system.actorOf(Props(new MetricProcessor)) ! (httpRequest, metricId, rest)
      case GET  (PathSegments("trail" :: messageId :: Nil))  => system.actorOf(Props(new TrailProcessor(database))) ! (httpRequest, decode(messageId, "UTF-8"))
      case POST (Path("/event"))                             => system.actorOf(Props(new EventProcessor(database))) ! httpRequest
    }
  })

  val webServer = new WebServer(webServerConfig, routes)
  
  private def getStaticFile(context: HttpRequestProcessingContext, path: String): StaticFileRequest = StaticFileRequest(
    context, StaticFileDir, new File(StaticFileDir, path), new File(System.getProperty("java.io.tmpdir"))
  )
  
  private def databaseActorType = Props(new Database(new com.bimbr.clisson.server.database.h2.H2Connector(config)))
  
  private def loadConfig(): Config = {
    val fileName = Option(System.getProperty("config")).getOrElse("clisson-server.properties")
    Config.fromPropertiesFile(fileName) match {
      case Left(msg)  => throw new IllegalStateException(msg)
      case Right(cfg) => cfg
    }
  } 

  def start() = {
    webServer.start()
    Runtime.getRuntime.addShutdownHook(new Thread("app-shutdown") {
      override def run { SockoApplication.stop() }
    })
    isRunning = true
  }
  
  def stop() = if (isRunning) {
    webServer.stop()
    system.shutdown()
    isRunning = false
  }
}