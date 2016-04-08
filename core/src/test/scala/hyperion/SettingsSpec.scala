package hyperion

import akka.actor.ActorSystem
import scala.concurrent.duration.DurationInt

class SettingsSpec extends BaseSpec {
  implicit val system = ActorSystem()
  val settings = Settings(system)

  "Inspecting 'api'" should {
    "return correct values for the 'receiver' configuration section" in {
      settings.api.port       shouldBe 8080
    }
  }

  "Inspecting 'history'" should {
    "return correct values" in {
      settings.history.resolution shouldBe (100 millis)
      settings.history.limit shouldBe (24 hours)
    }
  }
}
