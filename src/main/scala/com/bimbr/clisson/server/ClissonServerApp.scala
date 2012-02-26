package com.bimbr.clisson.server

/**
 * The entry point of the server.
 * 
 * @author mmakowski
 * @since 1.0.0
 */
object ClissonServerApp {
  def main(args: Array[String]) = play.core.server.NettyServer.main(args)
}