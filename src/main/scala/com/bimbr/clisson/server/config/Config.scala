package com.bimbr.clisson.server.config

import java.io.InputStream
import java.util.Properties

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
  /**
   * Creates a Config object from the classpath file provided
   * @since 1.0.0
   */
  def fromPropertiesFile(fileName: String): Option[Config] = 
    Option(Thread.currentThread.getContextClassLoader.getResourceAsStream(fileName)).
        map(propertiesFromStream).
        map(new Config(_))
  
  private def propertiesFromStream(stream: InputStream): Properties = {
    val properties = new Properties
    properties.load(stream)
    stream.close()
    properties
  } 
} 