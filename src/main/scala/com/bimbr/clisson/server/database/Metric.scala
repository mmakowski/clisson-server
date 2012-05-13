package com.bimbr.clisson.server.database

/**
 * Various metrics provided by the database. The classes use Java component types to allow for easy Gson serialisation.
 */
sealed trait Metric
/**
 * The average latency of messages, by component; example:
 * {{{
 * {
 *   "endToEndLatency": 1280, 
 *   "componentLatencies": {
 *     "c1":     750,
 *     "c2":     150
 *   }
 * )
 * }}}
 * 
 * @since 1.0.0
 */
case class AverageLatency(endToEndLatency: Long, componentLatencies: java.util.Map[String, Long]) extends Metric
/**
 * The throughput of the system, by component; example:
 * {{{
 * {
 *   "endToThroughput": 2.1709, 
 *   "componentThroughputs": {
 *     "c1":     2.1411,
 *     "c2":     2.2111
 *   }
 * )
 * }}}
 * 
 * @since 1.0.0
 */
case class Throughput(endToEndThroughput: Double, componentThroughputs: java.util.Map[String, Double]) extends Metric
