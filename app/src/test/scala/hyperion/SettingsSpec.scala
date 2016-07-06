package hyperion

import akka.actor.ActorSystem
import com.github.jodersky.flow.Parity
import scala.concurrent.duration.DurationInt

class SettingsSpec extends BaseSpec {
  implicit val system = ActorSystem()
  val settings = Settings(system)

  "Inspecting 'api'" should {
    "return correct values for the 'receiver' configuration section" in {
      settings.api.port            shouldBe 8080
    }
  }

  "Inspecting 'history'" should {
    "return correct values" in {
      settings.history.resolution  shouldBe (100 millis)
      settings.history.limit       shouldBe (24 hours)
    }
  }

  "Inspecting 'daily'" should {
    "return correct values" in {
      settings.daily.resolution    shouldBe (1 day)
    }
  }

  "Inspecting 'meter'" should {
    "return correct values for the 'meter' configuration section" in {
      settings.meter.serialPort    shouldBe "/dev/ttyUSB0"
      settings.meter.baudRate      shouldBe 115200
      settings.meter.characterSize shouldBe 8
      settings.meter.stopBits      shouldBe 1
      settings.meter.parity        shouldBe Parity.Odd
    }
  }
}
