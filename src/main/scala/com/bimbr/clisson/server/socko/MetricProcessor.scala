package com.bimbr.clisson.server.socko

import akka.actor.{ Actor, ActorRef }
import akka.pattern.ask
import akka.util.Timeout
import com.bimbr.clisson.server.database.{ AverageLatency, GetAverageLatency }
import org.jboss.netty.handler.codec.http.HttpResponseStatus.{ BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND, NOT_IMPLEMENTED }
import org.mashupbots.socko.context.HttpRequestProcessingContext


class MetricProcessor(database: ActorRef)(implicit val timeout: Timeout) extends Actor {
  def receive = {
    case (request: HttpRequestProcessingContext, metricId: String, id: List[String]) => {
      implicit val req = request
      metricId match {
        case "average-latency" => 
          handlingTimeout(await[Either[Throwable, AverageLatency]](database ? GetAverageLatency()) match {
            case Right(metric) => request.writeResponse(jsonFor(metric)) 
            case Left(t)       => request.writeErrorResponse(INTERNAL_SERVER_ERROR, msg = t.getMessage)
          })
        case _ => request.writeErrorResponse(NOT_FOUND, msg = "metric " + metricId + " is not supported") 
      }
    }
    context.stop(self)
  }
}
  
