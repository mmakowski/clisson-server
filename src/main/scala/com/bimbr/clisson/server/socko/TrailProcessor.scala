package com.bimbr.clisson.server.socko

import akka.actor.{ Actor, ActorRef }
import akka.pattern.ask
import akka.util.Timeout
import com.bimbr.clisson.protocol.Trail
import com.bimbr.clisson.server.database.GetTrail
import org.jboss.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND
import org.mashupbots.socko.context.HttpRequestProcessingContext

class TrailProcessor(database: ActorRef)(implicit val timeout: Timeout) extends Actor {
  def receive = {
    case (request: HttpRequestProcessingContext, messageId: String) =>
      implicit val req = request
      handlingTimeout(await[Option[Trail]](database ? GetTrail(messageId)) match {
        case Some(t) => request.writeResponse(jsonFor(t)) 
        case None    => request.writeErrorResponse(NOT_FOUND, msg = "trail for " + messageId + " not found")
      })
      context.stop(self)
  }
}
  
