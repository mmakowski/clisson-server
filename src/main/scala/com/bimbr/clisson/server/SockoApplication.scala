package com.bimbr.clisson.server

import akka.pattern.ask
import akka.util.Timeout
import akka.util.duration._
import akka.actor.{ ActorSystem, Props, Actor }
import akka.dispatch.{ Await, Future }
import com.bimbr.clisson.protocol.{ Event, Trail }
import com.bimbr.clisson.protocol.Json.jsonFor
import com.bimbr.clisson.protocol.Types.classFor
import com.bimbr.clisson.server.database.{ Database, Insert, GetTrail }
import com.bimbr.clisson.server.config.Config
import org.jboss.netty.handler.codec.http.HttpResponseStatus.{ ACCEPTED, BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND }
import org.mashupbots.socko.context.HttpRequestProcessingContext
import org.mashupbots.socko.routes.{ GET, HttpRequest, POST, Path, PathSegments, Routes }
import org.mashupbots.socko.webserver.{ WebServer, WebServerConfig }

object SockoApplication {
  private lazy val config = loadConfig()
  private lazy val webServerConfig = WebServerConfig(port = Integer.parseInt(System.getProperty("http.port", "9000")))
  private val system = ActorSystem("clissonServer");
  implicit val timeout = Timeout(10000 milliseconds)
  private val deserialiser = new Deserialiser
  private val database = system.actorOf(databaseActorType)
  
  /**
   * Defines how HTTP requests to different paths are handled.
   */
  val routes = Routes({
    case HttpRequest(httpRequest) => httpRequest match {
      case GET  (PathSegments("trail" :: messageId :: Nil)) => system.actorOf(Props[TrailProcessor]) ! (httpRequest, messageId)
      case POST (Path("/event"))                            => system.actorOf(Props[EventProcessor]) ! httpRequest
    }
  })

  class TrailProcessor extends Actor {
    def receive = {
      case (request: HttpRequestProcessingContext, messageId: String) =>        
        Await.result(database ? GetTrail(messageId), timeout.duration).asInstanceOf[Option[Trail]] match {
          case Some(t) => request.writeResponse(serialise(t)) 
          case None    => request.writeErrorResponse(NOT_FOUND, msg = "trail for " + messageId + " not found")
        }
        context.stop(self)
    }
  }
  
  class EventProcessor extends Actor {
    def receive = {
      case request: HttpRequestProcessingContext => try {
          val json = request.readStringContent
          deserialise(classOf[Event], json) match {
            case Left(event) => persist(event); request.writeErrorResponse(ACCEPTED)
            case Right(exc)  => request.writeErrorResponse(BAD_REQUEST, msg = exc.getMessage)
          }
        } catch {
          case e: Exception => request.writeErrorResponse(INTERNAL_SERVER_ERROR, msg = e.getMessage)
        }
        context.stop(self)
    }
  }
  
  private def serialise[T](obj: T) = jsonFor(obj)
  
  private def deserialise[T](cls: Class[T], json: String) = deserialiser.deserialise(cls, json)
  
  private def persist(event: Event): Unit = database ! Insert(event) 
    
  private def databaseActorType = Props(new Database(new com.bimbr.clisson.server.database.h2.H2Connector(config)))
  
  private def loadConfig(): Config = {
    val fileName = Option(System.getProperty("config")).getOrElse("clisson-server.properties")
    Config.fromPropertiesFile(fileName) match {
      case Left(msg)  => throw new IllegalStateException(msg)
      case Right(cfg) => cfg
    }
  } 

  def start() = {
    val webServer = new WebServer(webServerConfig, routes)
    webServer.start()
    Runtime.getRuntime.addShutdownHook(new Thread("webserver-shutdown") {
      override def run { webServer.stop() }
    })
  }
}