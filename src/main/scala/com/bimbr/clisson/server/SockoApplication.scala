package com.bimbr.clisson.server

import java.io.File
import java.net.URLDecoder.decode
import scala.collection.JavaConverters._
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
import com.typesafe.config.{ Config, ConfigFactory }
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
class SockoApplication extends ServerApplication {
  // get a logger to initialise logging system before other components require it to avoid http://www.slf4j.org/codes.html#substituteLogger
  LoggerFactory.getLogger("SockoApplication")
  private var isRunning = false

  private lazy val config = loadConfig()
  private lazy val webServerConfig = WebServerConfig(
    serverName = "ClissonWebServer",
    port       = config.getInt("http.port")
  )
    
  private val          StaticFileDir = new File("static")
  private lazy val     system = ActorSystem("clisson-server");
  private implicit val timeout = Timeout(10 seconds)
  private val          database = system.actorOf(databaseActorType, name = "database")
  private lazy val     staticFileProcessor = system.actorOf(Props[StaticFileProcessor].
                                                            withRouter(FromConfig()).
                                                            withDispatcher("pinned-dispatcher"), name = "static-file-router")
  
  /**
   * Defines how HTTP requests to different paths are handled.
   */
  private lazy val routes = Routes({
    case HttpRequest(httpRequest) => httpRequest match {
      case GET  (Path("/favicon.ico"))                       => staticFileProcessor ! getStaticFile(httpRequest, "favicon.ico") 
      case GET  (PathSegments("metric" :: metricId :: rest)) => actor(new MetricProcessor(database)) ! (httpRequest, metricId, rest)
      case GET  (PathSegments("trail" :: messageId :: Nil))  => actor(new TrailProcessor(database)) ! (httpRequest, decode(messageId, "UTF-8"))
      case POST (Path("/event"))                             => actor(new EventProcessor(database)) ! httpRequest
    }
  })

  private lazy val webServer = new WebServer(webServerConfig, routes)
  
  private def actor(construction: => Actor): ActorRef = system.actorOf(Props(construction))
  
  private def getStaticFile(context: HttpRequestProcessingContext, path: String): StaticFileRequest = StaticFileRequest(
    context, StaticFileDir, new File(StaticFileDir, path), new File(System.getProperty("java.io.tmpdir"))
  )
  
  private def databaseActorType = Props(new Database(new com.bimbr.clisson.server.database.h2.H2Connector(config)))
  
  private def loadConfig(): Config = {
    val configPath = Option(System.getProperty("config")).getOrElse("clisson-server.conf")
    ConfigFactory.parseFile(new File(configPath)).
                  withFallback(ConfigFactory.load(configPath)).
                  withFallback(ConfigFactory.parseMap(Map(
        "http.port" -> "9000"
      ).asJava)).resolve
  } 

  def start() = {
    webServer.start()
    Runtime.getRuntime.addShutdownHook(new Thread("app-shutdown") {
      override def run { SockoApplication.this.stop() }
    })
    isRunning = true
  }
  
  def stop() = if (isRunning) {
    webServer.stop()
    system.shutdown()
    isRunning = false
  }
}