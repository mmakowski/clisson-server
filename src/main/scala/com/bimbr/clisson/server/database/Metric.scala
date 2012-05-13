package com.bimbr.clisson.server.database

/**
 * Various metrics provided by the database.
 */
sealed trait Metric
/**
 * The average latency of messages, by component and connection; example:
 * {{{
 * AverageLatency(
 *   componentLatencies = {
 *     "c1"         -> 750,
 *     "c1->c2"     -> 400,
 *     "c2"         -> 150,
 *     "end-to-end" -> 1300
 *   }
 * )
 * }}}
 * 
 * @since 1.0.0
 */
case class AverageLatency(componentLatencies: Map[String, Long]) extends Metric
