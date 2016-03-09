package hyperion

import akka.actor.ActorSystem
import com.github.jodersky.flow.Parity

class SettingsSpec extends BaseSpec {
  implicit val system = ActorSystem()
  val settings = Settings(system)

  "Inspecting 'meter'" should {
    "return correct values for the 'meter' configuration section" in {
      settings.meter.serialPort    shouldBe "/dev/ttyUSB0"
      settings.meter.baudRate      shouldBe 115200
      settings.meter.characterSize shouldBe 8
      settings.meter.stopBits      shouldBe 1
      settings.meter.parity        shouldBe Parity.Odd
    }

    "return correct values for the 'receiver' configuration section" in {
      settings.receiver.host       shouldBe "localhost"
    }
  }
}
