package com.bimbr.clisson.server

import com.typesafe.play.mini.{ POST, GET, Path, Application }
import play.api.mvc.{ Action, AsyncResult, AnyContent, Request, Result }
import play.api.mvc.Results._
import play.api.libs.concurrent._
import play.api.data._
import play.api.data.Forms._
import akka.pattern.ask
import akka.util.Timeout
import akka.util.duration._
import akka.actor.{ ActorSystem, Props, Actor }

import com.bimbr.clisson.protocol.Event
import com.bimbr.clisson.protocol.Types.classFor
import com.bimbr.clisson.server.database.{ Database, Insert }

/**
 * The central point of the HTTP API.
 * 
 * @author mmakowski
 * @since 1.0.0
 */
object PlayApplication extends Application {
  private val system = ActorSystem("clissonServer");
  implicit val timeout = Timeout(1000 milliseconds)
  private val EventPattern = """/event/(\w+)""".r 
  private val deserialiser = new Deserialiser
  private val database = system.actorOf(databaseActorType)
  
  /**
   * Defines how HTTP requests to different paths are handled.
   */
  def route = {
    case POST(Path(EventPattern(eventType))) => Action(addEvent(_, eventType))
  }

  private def addEvent(implicit request: Request[AnyContent], eventType: String) = try {
    val cls = classFor(eventType).asInstanceOf[Class[Event]]
    request.body.asText match {
      case Some(json) => persist(deserialise(cls, json))
      case None       => BadRequest("the request body was empty; expected JSON of " + eventType)
    }
  } catch {
    case e: Exception => BadRequest(e.getMessage)
  }
  
  private def deserialise[T](cls: Class[T], json: String) = deserialiser deserialise (cls, json)
  
  private def persist(e: Either[Event, Exception]): Result = e match {
    case Left(event) => persist(event); Accepted
    case Right(exc)  => BadRequest(exc.getMessage)
  }
  
  private def persist(event: Event): Unit = database ! Insert(event) 
    
  private def databaseActorType = Props(new Database(new com.bimbr.clisson.server.database.h2.H2Connector))
}