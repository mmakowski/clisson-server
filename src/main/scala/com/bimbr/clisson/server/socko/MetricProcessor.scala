package com.bimbr.clisson.server.socko

import java.util.Date
import java.text.SimpleDateFormat
import scala.util.control.Exception.catching
import akka.actor.{ Actor, ActorRef }
import akka.dispatch.Await.Awaitable
import akka.pattern.ask
import akka.util.Timeout
import com.bimbr.clisson.server.database.{ AverageLatency, GetAverageLatency, GetThroughput, Throughput }
import org.jboss.netty.handler.codec.http.HttpResponseStatus.{ BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND, NOT_IMPLEMENTED }
import org.mashupbots.socko.context.HttpRequestProcessingContext


class MetricProcessor(database: ActorRef)(implicit val timeout: Timeout) extends Actor {
  def StartTime = "startTime"
  def EndTime   = "endTime"
  
  def receive = {
    case (request: HttpRequestProcessingContext, metricId: String, id: List[String]) => {
      metricId match {
        case "average-latency" => getAverageLatency(request)
        case "throughput"      => getThroughput(request)
        case _ => request.writeErrorResponse(NOT_FOUND, msg = "metric " + metricId + " is not supported") 
      }
    }
    context.stop(self)
  }

  // average latency
  case class AverageLatencyParams(startTime: Option[Date], endTime: Option[Date])
  
  def getAverageLatency(request: HttpRequestProcessingContext) = {
    implicit val req = request
    averageLatencyParams match {
      case Left(errorMsg) => request.writeErrorResponse(BAD_REQUEST, msg = errorMsg) 
      case Right(params)  => respondWithJson(database ? GetAverageLatency(params.startTime, params.endTime))
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
  
  // throughput
  case class ThroughputParams(startTime: Date, endTime: Date)
  
  def getThroughput(request: HttpRequestProcessingContext) = {
    implicit val req = request
    throughputParams match {
      case Left(errorMsg) => request.writeErrorResponse(BAD_REQUEST, msg = errorMsg) 
      case Right(params)  => respondWithJson(database ? GetThroughput(params.startTime, params.endTime))
    } 
  }
  
  def throughputParams(implicit request: HttpRequestProcessingContext): Either[String, ThroughputParams] =
    queryParam(StartTime).map(parseIso8601Timestamp) match {
      case Some(Left(e))  => Left("error parsing " + StartTime +" query parameter: " + e.getMessage)
      case Some(Right(d)) => throughputParams(d)
      case None           => Left(StartTime + " query parameter missing")
    }

  def throughputParams(startTime: Date)(implicit request: HttpRequestProcessingContext): Either[String, ThroughputParams] =
    queryParam(EndTime).map(parseIso8601Timestamp) match {
      case Some(Left(e))  => Left("error parsing " + EndTime +" query parameter: " + e.getMessage)
      case Some(Right(d)) => Right(ThroughputParams(startTime, d))
      case None           => Left(EndTime + " query parameter missing")
    }
  

  def respondWithJson[T](awaitable: => Awaitable[T])(implicit request: HttpRequestProcessingContext) = 
    handlingTimeout(await[Either[Throwable, Any]](awaitable) match {
      case Right(metric) => request.writeResponse(jsonFor(metric)) 
      case Left(t)       => request.writeErrorResponse(INTERNAL_SERVER_ERROR, msg = t.getMessage)
    })
  
  def queryParam(id: String)(implicit request: HttpRequestProcessingContext): Option[String] = 
    catching(classOf[Exception]) opt request.endPoint.queryStringMap(id).get(0)
  
  // extract to common utils module
  val ISO8601 = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
  
  def parseIso8601Timestamp(str: String): Either[Throwable, Date] = catching (classOf[Throwable]) either {
    new SimpleDateFormat(ISO8601).parse(str)
  }
}
  
