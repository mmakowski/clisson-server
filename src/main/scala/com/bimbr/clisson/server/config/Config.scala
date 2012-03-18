package com.bimbr.clisson.server.config

import java.io.InputStream
import java.util.Properties

import org.apache.commons.configuration.{ CompositeConfiguration, PropertiesConfiguration, SystemConfiguration }

import scalaz._

/**
 * Provides the server configuration.
 * @author mmakowski
 * @since 1.0.0
 */
class Config(properties: Properties, val fileName: String) {
  val config = new CompositeConfiguration
  config.addConfiguration(new SystemConfiguration)
  config.addConfiguration(new PropertiesConfiguration(fileName))
  
  def apply(propertyKey: String): Option[String] = Option(config.getString(propertyKey))
  override def toString(): String = "server config loaded from " + fileName 
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
  def fromPropertiesFile(fileName: String): Either[String, Config] = {
    for {
      stream <- streamFromClasspath(fileName)
      props  <- propertiesFromStream(stream)
    } yield new Config(props, fileName)
  }.either
  
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