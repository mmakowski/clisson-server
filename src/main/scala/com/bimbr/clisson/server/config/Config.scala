package com.bimbr.clisson.server.config

import java.io.InputStream
import java.util.Properties

import scalaz._

/**
 * Provides the server configuration.
 * @author mmakowski
 * @since 1.0.0
 */
class Config(val properties: Properties) {
  def apply(propertyKey: String): Option[String] = Option(properties.getProperty(propertyKey)) 
}

/**
 * Provides functions that create Config objects
 * @author mmakowski
 * @since 1.0.0
 */
object Config {
  import Scalaz._
  
  /**
   * Creates a Config object from the classpath file provided.
   * @return Left(errorMessage) if an error has occurred or Right(config) if config was created succesfully
   * @since 1.0.0
   */
  def fromPropertiesFile(fileName: String): Either[String, Config] = (for {
    stream <- streamFromClasspath(fileName)
    props  <- propertiesFromStream(stream)
  } yield new Config(props)).either
  
  private def streamFromClasspath(fileName: String): Validation[String, InputStream] = 
    Thread.currentThread.getContextClassLoader.getResourceAsStream(fileName) match {
      case null   => failure("config file " + fileName + " not found in the classpath") 
      case stream => success(stream)
    }

  private def propertiesFromStream(stream: InputStream): Validation[String, Properties] = try {
    val properties = new Properties
    properties.load(stream)
    stream.close()
    success(properties)
  } catch {
    case e => failure(e.getMessage)
  }
} 