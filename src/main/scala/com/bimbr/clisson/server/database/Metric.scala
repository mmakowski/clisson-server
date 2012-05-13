package com.bimbr.clisson.server.database

/**
 * Various metrics provided by the database. The classes use Java component types to allow for easy Gson serialisation.
 */
sealed trait Metric
/**
 * The average latency of messages, by component and connection; example:
 * {{{
 * {
 *   "endToEndLatency": 1280, 
 *   "componentLatencies": {
 *     "c1":     750,
 *     "c1->c2": 400,
 *     "c2":     150
 *   }
 * )
 * }}}
 * 
 * @since 1.0.0
 */
case class AverageLatency(endToEndLatency: Long, componentLatencies: java.util.Map[String, Long]) extends Metric
