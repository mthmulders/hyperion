package hyperion

import akka.actor.ActorSystem
import com.github.jodersky.flow.Parity
import scala.concurrent.duration.DurationInt

class SettingsSpec extends BaseSpec {
  implicit val system = ActorSystem()
  val settings = Settings(system)

  "Inspecting 'api'" should {
    "return correct values" in {
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
      settings.daily.resolution    shouldBe (100 millis)
    }
  }

  "Inspecting 'database'" should {
    "return correct values" in {
      settings.database.driver     shouldBe "org.postgresql.Driver"
      // others vary with environment, so are not tested here
    }
  }

  "Inspecting 'meter'" should {
    "return correct values" in {
      settings.meter.serialPort    shouldBe "/dev/ttyUSB0"
      settings.meter.baudRate      shouldBe 115200
      settings.meter.characterSize shouldBe 8
      settings.meter.stopBits      shouldBe 1
      settings.meter.parity        shouldBe Parity.Odd
    }
  }
}
