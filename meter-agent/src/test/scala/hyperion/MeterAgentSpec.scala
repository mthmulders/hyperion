package hyperion

import akka.testkit.{EventFilter, TestProbe}
import com.github.jodersky.flow.Serial
import com.github.jodersky.flow.Serial.Open

class MeterAgentSpec extends BaseAkkaSpec {
  "Creating the Meter Agent" should {
    "result in creating a IO-SERIAL system actor" in {
      system.actorOf(MeterAgent.props(), "create-system-actor")
      TestProbe().expectActor("/system/IO-SERIAL")
    }
  }

  "Receiving the \"CommandFailed\" message" should {
    "log the failed command and the reason as an error" in {
      val actor = system.actorOf(MeterAgent.props(), "log-command-failure")
      EventFilter[IllegalArgumentException](pattern = "Could not open serial port due to IllegalArgumentException", occurrences = 1) intercept {
        val reason = new IllegalArgumentException("test")
        actor ! Serial.CommandFailed(Open("", null, 1), reason)
      }
    }
  }

  "Receiving the \"Opened\" message" should {
    "log having opened the port at info" in {
      val actor = system.actorOf(MeterAgent.props(), "log-port-opened")
      EventFilter.info("Opened serial port /foo", occurrences = 1) intercept {
        actor ! Serial.Opened("/foo")
      }
    }
  }
}
