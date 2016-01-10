package hyperion

import akka.testkit.{EventFilter, TestProbe}
import akka.util.ByteString
import com.github.jodersky.flow.Serial
import com.github.jodersky.flow.Serial.Open

import scala.concurrent.duration.DurationInt

class MeterAgentSpec extends BaseAkkaSpec {
  private val dummy = TestProbe().ref

  "Creating the Meter Agent" should {
    "result in creating a IO-SERIAL system actor" in {
      system.actorOf(MeterAgent.props(dummy), "create-system-actor")
      TestProbe().expectActor("/system/IO-SERIAL", 250 milliseconds)
    }
  }

  "Receiving the \"CommandFailed\" message" should {
    "log the failed command and the reason as an error" in {
      val actor = system.actorOf(MeterAgent.props(dummy), "log-command-failure")
      EventFilter[IllegalArgumentException](pattern = "Could not open serial port due to IllegalArgumentException", occurrences = 1) intercept {
        val reason = new IllegalArgumentException("test")
        actor ! Serial.CommandFailed(Open("", null, 1), reason)
      }
    }
  }

  "Receiving the \"Opened\" message" should {
    "log having opened the port at info" in {
      val actor = system.actorOf(MeterAgent.props(dummy), "log-port-opened")
      EventFilter.info("Opened serial port /foo", occurrences = 1) intercept {
        actor ! Serial.Opened("/foo")
      }
    }
  }

  "Receiving the \"Received\" message" should {
    "convert the bytes to a String and forward them to the CollectingActor" in {
      val collectingActor = TestProbe()
      val actor = system.actorOf(MeterAgent.props(collectingActor.ref), "forward-string-to-collectingactor")
      val bytes = ByteString(41, 13, 10)
      actor ! Serial.Received(bytes)

      collectingActor expectMsg MeterAgent.IncomingData(")\r\n")
    }
  }
}
