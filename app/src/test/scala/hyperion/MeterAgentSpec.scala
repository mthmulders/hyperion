package hyperion

import akka.actor.Props
import akka.testkit.{EventFilter, TestProbe}
import akka.util.ByteString
import com.github.jodersky.flow.{Serial, SerialSettings}
import com.github.jodersky.flow.Serial.Open
import org.scalatest.mock.MockitoSugar

import scala.concurrent.duration.DurationInt

class MeterAgentSpec extends BaseAkkaSpec with MockitoSugar {
  private val dummy = TestProbe().ref

  "Creating the Meter Agent" should {
    "result in creating a IO-SERIAL system actor" in {
      system.actorOf(Props(new MeterAgent(dummy, settings)), "create-system-actor")
      TestProbe().expectActor("/system/IO-SERIAL", 2 seconds) should not be empty
    }
  }

  "Receiving the \"CommandFailed\" message" should {
    "log the failed command and the reason as an error" in {
      val actor = system.actorOf(Props(new MeterAgent(dummy, settings)), "log-command-failure")
      EventFilter[IllegalArgumentException](pattern = "Could not open serial port due to IllegalArgumentException", occurrences = 1) intercept {
        val reason = new IllegalArgumentException("test")
        actor ! Serial.CommandFailed(Open("", mock[SerialSettings], 1), reason)
      }
    }
  }

  "Receiving the \"Opened\" message" should {
    "log having opened the port at info" in {
      val actor = system.actorOf(Props(new MeterAgent(dummy, settings)), "log-port-opened")
      EventFilter.info("Opened serial port /foo", occurrences = 1) intercept {
        actor ! Serial.Opened("/foo")
      }
    }
  }

  "Receiving the \"Received\" message" should {
    "convert the bytes to a String and forward them to the CollectingActor" in {
      val collectingActor = TestProbe()
      val actor = system.actorOf(Props(new MeterAgent(collectingActor.ref, settings)), "forward-string-to-collectingactor")
      val bytes = ByteString(41, 13, 10)
      actor ! Serial.Received(bytes)

      collectingActor expectMsg MeterAgent.IncomingData(")\r\n")
    }
  }
}
