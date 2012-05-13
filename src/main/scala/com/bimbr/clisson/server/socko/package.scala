package com.bimbr.clisson.server

import java.util.concurrent.TimeoutException
import akka.dispatch.Await
import akka.util.Timeout
import org.mashupbots.socko.context.HttpRequestProcessingContext
import org.jboss.netty.handler.codec.http.HttpResponseStatus.{ REQUEST_TIMEOUT }

/**
 * This package contains Socko processors for various types of requests. They are used by com.bimbr.clisson.server.SockoApplication.
 */
package object socko {
  /**
   * handles java.util.concurrent.TimeoutException by responding to HTTP request with REQUEST_TIMEOUT
   */
  def handlingTimeout[T](body: => T)(implicit request: HttpRequestProcessingContext) = try {
    body
  } catch {
    case e: TimeoutException => request.writeErrorResponse(REQUEST_TIMEOUT, msg = e.getMessage())
  }
  
  /**
   * convenience wrapper for Await.result with implicit timeout and built-in cast
   */
  def await[T](awaitable: => akka.dispatch.Await.Awaitable[Any])(implicit timeout: Timeout): T =
      Await.result(awaitable, timeout.duration).asInstanceOf[T]

  def jsonFor[T](obj: T) = com.bimbr.clisson.protocol.Json.jsonFor(obj)
}
