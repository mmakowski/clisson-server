package integration

import com.bimbr.clisson.client.RecorderFactory.getRecorder
import org.apache.http.util.EntityUtils
import scala.annotation.tailrec
import System.currentTimeMillis


object performanceTest extends IntegrationTest{
  val BatchTimeoutMs = 30000 
  val BatchSize = 1000 // will fit in async recorder buffer
  val PollDelayMs = 50 // higher increases save preformance (i.e. more realistic) but reduces timing accuracy
  
  val serverConfig = "integration/performance/clisson-server.properties"

  lazy val record = {
    System.setProperty("clisson.config", "classpath://integration/performance/clisson-client.properties")
    getRecorder("performanceTest")
  }
    
  def run(server: Thread) = (0 to 99) foreach { runBatch }

  private def runBatch(batchNo: Int) = {
    val startTime = currentTimeMillis()
    val startIndex = BatchSize * batchNo
    val endIndex = BatchSize * (batchNo+1) - 1
    (startIndex to endIndex) foreach { recordEvent }
    val sendTime = currentTimeMillis() - startTime
    val queryTime = waitUntilMessageIsAvailable(msgId(endIndex))
    val totalTime = currentTimeMillis() - startTime
    println("times for batch " + batchNo + ": send=" + sendTime + " / query=" + queryTime.getOrElse("TIMEOUT!") + " / total=" + 
            (if (queryTime.isDefined) totalTime else "TIMEOUT!"))
  }
  
  // return none if timed out
  private def waitUntilMessageIsAvailable(msgId: String): Option[Long] = {
    val waitStartTime = currentTimeMillis()
    def timedOut = currentTimeMillis() - waitStartTime >= BatchTimeoutMs
    var queryTime = None: Option[Long]
    while (!queryTime.isDefined && !timedOut) {
      val queryStartTime = currentTimeMillis()
      if (trail(msgId).getStatusLine.getStatusCode == 200) queryTime = Some(currentTimeMillis() - queryStartTime)
      if (!queryTime.isDefined) Thread.sleep(PollDelayMs) // throttle polling
    }
    queryTime
  }
  
  private def recordEvent(i: Int) = record.checkpoint(msgId(i), "the description of processing message " + i)
  
  private def msgId(i: Int) = "msg-" + i
}