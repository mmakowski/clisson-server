package com.bimbr.clisson.server.socko

import scala.util.control.Exception.catching
import akka.actor.{ Actor, ActorRef }
import com.bimbr.clisson.protocol.Event
import com.bimbr.clisson.protocol.Json.fromJson
import com.bimbr.clisson.server.database.Insert
import org.jboss.netty.handler.codec.http.HttpResponseStatus.{ ACCEPTED, BAD_REQUEST, INTERNAL_SERVER_ERROR }
import org.mashupbots.socko.context.HttpRequestProcessingContext

class EventProcessor(database: ActorRef) extends Actor {
  def receive = {
    case request: HttpRequestProcessingContext => try {
        val json = request.readStringContent
        deserialise(classOf[Event], json) match {
          case Right(event) => persist(event); request.writeErrorResponse(ACCEPTED)
          case Left(exc)    => request.writeErrorResponse(BAD_REQUEST, msg = exc.getMessage)
        }
      } catch {
        case e: Exception => request.writeErrorResponse(INTERNAL_SERVER_ERROR, msg = e.getMessage)
      }
      context.stop(self)
  }

  private def persist(event: Event): Unit = database ! Insert(event) 

  private def deserialise[T](cls: Class[T], json: String): Either[Throwable, T] = catching(classOf[Exception]) either fromJson(json, cls)
}

