package com.bimbr.clisson.server.database

import java.util.concurrent.TimeUnit.{ MILLISECONDS, SECONDS }
import java.util.Date
import akka.actor.{ ActorSystem, UnhandledMessage }
import akka.testkit.TestActorRef
import akka.util.duration._
import akka.util.{ Duration, Timeout }
import akka.dispatch.Await
import akka.pattern.ask
import org.junit.runner.RunWith
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner


@RunWith(classOf[JUnitRunner])
class DatabaseSpec extends Specification with Mockito {
  private implicit val system = ActorSystem("database-test");
  private implicit val timeout = Timeout(Duration(500, MILLISECONDS))
  
  "Database actor" should {
    "invoke trimming when TrimEventsBefore message is received" in {
      val cutOffTime = new Date(1L)
      val connector = mock[Connector]
      val connection = mock[Connection]
      connector.connect() returns (Right(connection))
      connection.trimEventsBefore(cutOffTime) returns (42)
      val db = TestActorRef(new Database(connector))
      // nice syntax "5 seconds" doesn't work because of ambiguous implicits
      val result = Await.result(db ? TrimEventsBefore(cutOffTime), Duration(1, SECONDS)).asInstanceOf[Int]
      result mustEqual 42
    } 
  }
}