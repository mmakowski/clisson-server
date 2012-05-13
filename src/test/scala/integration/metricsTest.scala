package integration

import java.net.URLEncoder.encode
import java.util.Date
import scala.collection.JavaConverters._
import com.bimbr.clisson.client.RecorderFactory
import com.bimbr.clisson.protocol.Event 
import com.bimbr.clisson.server.ServerApplication
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIUtils
import org.apache.http.util.EntityUtils


object metricsTest extends IntegrationTest {
  val Component1 = "component-1"
  val Component2 = "component-2"
  val Msg1 = Set("msg-1").asJava
  val Msg2 = Set("msg-2").asJava
  val Msg3 = Set("msg-3").asJava
  val serverConfig = "integration/metrics/clisson-server.properties"
  System.setProperty("clisson.config", "classpath://integration/metrics/clisson-client.properties")
  
  def run(server: ServerApplication) = {
    val record = RecorderFactory.getRecorder()
    record.event(new Event(Component1, new Date(0L), Msg1, Msg1, "in"))
    record.event(new Event(Component1, new Date(500L), Msg2, Msg2, "in"))
    record.event(new Event(Component1, new Date(1000L), Msg1, Msg1, "out"))
    record.event(new Event(Component1, new Date(1000L), Msg2, Msg2, "out"))
    record.event(new Event(Component1, new Date(1250L), Msg3, Msg3, "in"))
    record.event(new Event(Component2, new Date(1300L), Msg2, Msg2, "in"))
    record.event(new Event(Component2, new Date(1500L), Msg1, Msg1, "in"))
    record.event(new Event(Component2, new Date(1600L), Msg2, Msg2, "out"))
    record.event(new Event(Component2, new Date(2000L), Msg1, Msg1, "out"))
    record.event(new Event(Component1, new Date(2000L), Msg3, Msg3, "out"))
    record.event(new Event(Component2, new Date(2400L), Msg3, Msg3, "in"))
    record.event(new Event(Component2, new Date(2500L), Msg3, Msg3, "out"))
    Thread.sleep(1000)
    check("/metric/average-latency")
    // expected latencies:
    // c1:     750
    // c1->c2: 400
    // c2:     300
    check("/metric/average-latency", Some("startTime=" + encode("1970-01-01T00:00:02.000+0000", "UTF-8") + "&endTime=" + encode("1970-01-01T00:00:03.000+0000", "UTF-8")))
    // expected latencies:
    // c2:     100
  }

  def check(path: String, query: Option[String] = None) = {
    val response = responseTo(path, query)
    println("response for /metric/average-latency: " + response + "\n" + EntityUtils.toString(response.getEntity))
  }
  
  def responseTo(path: String, query: Option[String] = None) = {
    val client = new DefaultHttpClient
    val request = new HttpGet(URIUtils.createURI("http", "localhost", 9000, path, query.getOrElse(""), null))
    client.execute(request)
  }
}