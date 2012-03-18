package com.bimbr.clisson.server.config

import java.io.InputStream
import java.util.Properties

import org.apache.commons.configuration.{ CompositeConfiguration, PropertiesConfiguration, SystemConfiguration }

/**
 * Provides the server configuration.
 * @author mmakowski
 * @since 1.0.0
 */
class Config(fileName: String) {
  val config = new CompositeConfiguration
  config.addConfiguration(new SystemConfiguration)
  config.addConfiguration(new PropertiesConfiguration(fileName))
  
  /**
   * @return Some(value) if config setting is defined for propertyKey; None otherwise
   */
  def apply(propertyKey: String): Option[String] = Option(config.getString(propertyKey))
  
  override def toString(): String = "server config loaded from " + fileName 
}

/**
 * Provides functions that create Config objects
 * @author mmakowski
 * @since 1.0.0
 */
object Config {
  /**
   * Creates a Config object from the classpath file provided.
   * @return Left(errorMessage) if an error has occurred or Right(config) if config was created succesfully
   * @since 1.0.0
   */
  def fromPropertiesFile(fileName: String): Either[String, Config] = try {
    Right(new Config(fileName))
  } catch {
    case e => Left(e.getMessage())
  }
} 