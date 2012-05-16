package tests

import java.net.URLEncoder
import com.bimbr.clisson.client.RecorderFactory
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner


@RunWith(classOf[JUnitRunner])
class UrlEncodingSpec extends framework.ClissonServerSpecification { def is =
  "id in GET /trail/id is URL-decoded" ! withServer {
    val idWithSpecialChars = "SWAPTION|124100P/14"
    val record = RecorderFactory.getRecorder()
    record.checkpoint(idWithSpecialChars, "test")
    Thread.sleep(1000) // TODO: configure client to send data more frequently
    val encodedId = URLEncoder.encode(idWithSpecialChars, "UTF-8")
    trail(encodedId).getStatusLine.getStatusCode mustEqual 200 // OK
  }
}
  