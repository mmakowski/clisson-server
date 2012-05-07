package com.bimbr.clisson.server

/**
 * Interface for classes that run the server
 * 
 * @since 1.0.0
 * @author mmakowski
 */
trait ServerApplication {
  def start(): Unit
  def stop(): Unit
}
