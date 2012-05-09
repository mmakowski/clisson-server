package integration

import java.net.URLEncoder
import com.bimbr.clisson.server.config.Config
import org.apache.http.util.EntityUtils
import com.bimbr.clisson.client.RecorderFactory

object urlEncodingTest extends IntegrationTest {
  val serverConfig = "integration/urlEncoding/clisson-server.properties"
  val idWithSpecialChars = "SWAPTION|124100P/14"
  System.setProperty("clisson.config", "classpath://integration/urlEncoding/clisson-client.properties")
    
  def run(server: Thread) = {
    val record = RecorderFactory.getRecorder("url-encoding-test")
    record.checkpoint(idWithSpecialChars, "test")
    Thread.sleep(1000)
    checkTrail()
  }

  def checkTrail() = {
    val encodedId = URLEncoder.encode(idWithSpecialChars, "UTF-8")
    val response = trail(encodedId)
    println("response for /trail/" + encodedId + ": " + response + "\n" + EntityUtils.toString(response.getEntity))
  }
}