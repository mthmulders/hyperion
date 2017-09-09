package hyperion

import akka.actor.Props
import akka.testkit.{EventFilter, TestProbe}
import akka.util.ByteString
import com.github.jodersky.flow.{Serial, SerialSettings}
import com.github.jodersky.flow.Serial.Open
import org.scalamock.scalatest.MockFactory
import org.scalatest.OneInstancePerTest

import scala.concurrent.duration.DurationInt

class MeterAgentSpec extends BaseAkkaSpec with OneInstancePerTest with MockFactory {
  private val collectingActor = TestProbe()

  private val ma = system.actorOf(Props(new MeterAgent(collectingActor.ref, settings)), "meter-agent")

  "The Meter Agent actor" should {
    "create the IO-SERIAL system actor" in {
      TestProbe().expectActor("/system/IO-SERIAL", 2 seconds) should not be empty
    }

    "when it receives a \"CommandFailed\" message" should {
      "log the failed command and the reason as an error" in {
        EventFilter.error("Could not open serial port due to IllegalArgumentException", occurrences = 1) intercept {
          val reason = new IllegalArgumentException("test")
          ma ! Serial.CommandFailed(Open("", mock[SerialSettings], 1), reason)
        }
      }
    }

    "when it receives an \"Opened\" message" should {
      "log having opened the port at info" in {
        EventFilter.info("Opened serial port /foo", occurrences = 1) intercept {
          ma ! Serial.Opened("/foo")
        }
      }
    }

    "when it receives a \"Received\" message" should {
      "convert the bytes to a String and forward them to the CollectingActor" in {
        val bytes = ByteString(41, 13, 10)
        ma ! Serial.Received(bytes)

        collectingActor expectMsg MeterAgent.IncomingData(")\r\n")
      }
    }
  }
}
