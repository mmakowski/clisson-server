package com.bimbr.clisson.server.socko

import java.util.Date
import java.text.SimpleDateFormat
import scala.util.control.Exception.catching
import akka.actor.{ Actor, ActorRef }
import akka.pattern.ask
import akka.util.Timeout
import com.bimbr.clisson.server.database.{ AverageLatency, GetAverageLatency }
import org.jboss.netty.handler.codec.http.HttpResponseStatus.{ BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND, NOT_IMPLEMENTED }
import org.mashupbots.socko.context.HttpRequestProcessingContext


class MetricProcessor(database: ActorRef)(implicit val timeout: Timeout) extends Actor {
  def StartTime = "startTime"
  def EndTime   = "endTime"
  
  def receive = {
    case (request: HttpRequestProcessingContext, metricId: String, id: List[String]) => {
      metricId match {
        case "average-latency" => getAverageLatency(request)
        case _ => request.writeErrorResponse(NOT_FOUND, msg = "metric " + metricId + " is not supported") 
      }
    }
    context.stop(self)
  }

  case class AverageLatencyParams(startTime: Option[Date], endTime: Option[Date])
  
  def getAverageLatency(request: HttpRequestProcessingContext) = {
    implicit val req = request
    averageLatencyParams match {
      case Left(errorMsg) => request.writeErrorResponse(BAD_REQUEST, msg = errorMsg) 
      case Right(params)  =>
        handlingTimeout(await[Either[Throwable, AverageLatency]](database ? GetAverageLatency(params.startTime, params.endTime)) match {
          case Right(metric) => request.writeResponse(jsonFor(metric)) 
          case Left(t)       => request.writeErrorResponse(INTERNAL_SERVER_ERROR, msg = t.getMessage)
        })
    } 
  }
  
  def averageLatencyParams(implicit request: HttpRequestProcessingContext): Either[String, AverageLatencyParams] =
    // TODO: scalaz validation?
    queryParam(StartTime).map(parseIso8601Timestamp) match {
      case Some(Left(e))  => Left("error parsing " + StartTime +" query parameter: " + e.getMessage)
      case Some(Right(d)) => averageLatencyParams(Some(d))
      case None           => averageLatencyParams(None)
    }

  def averageLatencyParams(startTime: Option[Date])(implicit request: HttpRequestProcessingContext): Either[String, AverageLatencyParams] =
    queryParam(EndTime).map(parseIso8601Timestamp) match {
      case Some(Left(e))  => Left("error parsing " + EndTime +" query parameter: " + e.getMessage)
      case Some(Right(d)) => Right(AverageLatencyParams(startTime, Some(d)))
      case None           => Right(AverageLatencyParams(startTime, None))
    }
  
  def queryParam(id: String)(implicit request: HttpRequestProcessingContext): Option[String] = 
    catching(classOf[Exception]) opt request.endPoint.queryStringMap(id).get(0)
  
  // extract to common utils module
  val ISO8601 = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
  
  def parseIso8601Timestamp(str: String): Either[Throwable, Date] = catching (classOf[Throwable]) either {
    new SimpleDateFormat(ISO8601).parse(str)
  }
}
  
