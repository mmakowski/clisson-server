package com.bimbr.clisson.server.socko

import akka.actor.{ Actor }
import org.jboss.netty.handler.codec.http.HttpResponseStatus.{ ACCEPTED, BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND, NOT_IMPLEMENTED }
import org.mashupbots.socko.context.HttpRequestProcessingContext


class MetricProcessor extends Actor {
  def receive = {
    case (request: HttpRequestProcessingContext, metricId: String, id: List[String]) =>
      request.writeErrorResponse(NOT_IMPLEMENTED, msg = "metric " + metricId + " not implemented yet")
      context.stop(self)
  }
}
  
