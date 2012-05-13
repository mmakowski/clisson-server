package com.bimbr.clisson.server.socko

import java.util.concurrent.TimeoutException
import akka.actor.{ Actor, ActorRef }
import akka.dispatch.{ Await }
import akka.pattern.ask
import akka.util.Timeout
import akka.util.duration._
import com.bimbr.clisson.protocol.Trail
import com.bimbr.clisson.protocol.Json.jsonFor
import com.bimbr.clisson.server.database.GetTrail
import org.jboss.netty.handler.codec.http.HttpResponseStatus.{ NOT_FOUND, REQUEST_TIMEOUT }
import org.mashupbots.socko.context.HttpRequestProcessingContext

class TrailProcessor(database: ActorRef)(implicit val timeout: Timeout) extends Actor {
  def receive = {
    case (request: HttpRequestProcessingContext, messageId: String) =>
      try {
        Await.result(database ? GetTrail(messageId), timeout.duration).asInstanceOf[Option[Trail]] match {
          case Some(t) => request.writeResponse(serialise(t)) 
          case None    => request.writeErrorResponse(NOT_FOUND, msg = "trail for " + messageId + " not found")
        }
      } catch {
        case e: TimeoutException => request.writeErrorResponse(REQUEST_TIMEOUT, msg = "the database has not responded in a timely fashion")
      }
      context.stop(self)
  }
  
  private def serialise[T](obj: T): String = jsonFor(obj)
}
  
