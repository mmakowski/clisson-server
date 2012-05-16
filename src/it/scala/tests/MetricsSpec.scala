package tests

import java.net.URLEncoder.encode
import java.util.Date
import scala.collection.JavaConverters._
import com.bimbr.clisson.client.RecorderFactory
import com.bimbr.clisson.protocol.Event 
import com.bimbr.clisson.protocol.Json.fromJson
import com.bimbr.clisson.server.database.{ AverageLatency, Throughput }
import framework.ClissonServerSpecification
import org.apache.http.HttpResponse
import org.apache.http.util.EntityUtils
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.specification.Step

@RunWith(classOf[JUnitRunner])
class MetricsSpec extends ClissonServerSpecification { def is = sequential ^
  "For a predefined set of messages in the database"            ^ Step(startServer) ^ Step(prepareDb) ^
    "average-latency is as expected"                            ! averageLatency(query = None,
                                                                                 endToEndLatency = 1450, 
                                                                                 componentLatencies = Map(Component1 -> 750L,
                                                                                                          Component2 -> 300L)) ^
    "average-latency with time window specified is as expected" ! averageLatency(query = timeWindowQuery,
                                                                                 endToEndLatency = 500, 
                                                                                 componentLatencies = Map(Component2 -> 100L)) ^ 
    "throughput is as expected"                                 ! throughput(query = timeWindowQuery,
                                                                             endToEndThroughput = 2.0,
                                                                             componentThroughputs = Map(Component1 -> 1.0,
                                                                                                        Component2 -> 2.0)) ^
                                                                Step(stopServer) ^
                                                                end

                                                       
  // when read from Json all number are doubles, so need to cheat with the types a bit
  def averageLatency(query: Option[String], endToEndLatency: Long, componentLatencies: Map[String, Double]) = {
      val json = jsonFrom("/metric/average-latency", query)
      val retrievedLatency = fromJson(json, classOf[AverageLatency]) 
      retrievedLatency mustEqual AverageLatency(endToEndLatency = endToEndLatency,
                                                componentLatencies = componentLatencies.asInstanceOf[Map[String, Long]].asJava)  
  }
    
  def throughput(query: Option[String], endToEndThroughput: Double, componentThroughputs: Map[String, Double]) = {
      val json = jsonFrom("/metric/throughput", query)
      val retrievedThroughput = fromJson(json, classOf[Throughput]) 
      retrievedThroughput mustEqual Throughput(endToEndThroughput = endToEndThroughput,
                                               componentThroughputs = componentThroughputs.asJava)  
  }
  
  def jsonFrom(path: String, query: Option[String]) = {
      val response = responseTo(path, query)
      EntityUtils.toString(response.getEntity)
  }
  
  val timeWindowQuery = Some("startTime=" + encode("1970-01-01T00:00:02.000+0000", "UTF-8") + "&endTime=" + encode("1970-01-01T00:00:03.000+0000", "UTF-8"))

  val Component1 = "component-1"
  val Component2 = "component-2"
    
  def prepareDb() = {
    val Msg1 = Set("msg-1").asJava
    val Msg2 = Set("msg-2").asJava
    val Msg3 = Set("msg-3").asJava
    val record = RecorderFactory.getRecorder()
    record.event(new Event(Component1, new Date(0L   ), Msg1, Msg1, "in" ))
    record.event(new Event(Component1, new Date(500L ), Msg2, Msg2, "in" ))
    record.event(new Event(Component1, new Date(1000L), Msg1, Msg1, "out"))
    record.event(new Event(Component1, new Date(1000L), Msg2, Msg2, "out"))
    record.event(new Event(Component1, new Date(1250L), Msg3, Msg3, "in" ))
    record.event(new Event(Component2, new Date(1300L), Msg2, Msg2, "in" ))
    record.event(new Event(Component2, new Date(1500L), Msg1, Msg1, "in" ))
    record.event(new Event(Component2, new Date(1600L), Msg2, Msg2, "out"))
    record.event(new Event(Component2, new Date(2000L), Msg1, Msg1, "out"))
    record.event(new Event(Component1, new Date(2000L), Msg3, Msg3, "out"))
    record.event(new Event(Component2, new Date(2400L), Msg3, Msg3, "in" ))
    record.event(new Event(Component2, new Date(2500L), Msg3, Msg3, "out"))
    Thread.sleep(1000)
  }
    
}