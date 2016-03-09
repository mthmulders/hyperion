package hyperion

import akka.actor.ActorSystem

class SettingsSpec extends BaseSpec {
  implicit val system = ActorSystem()
  val settings = Settings(system)

  "Inspecting 'api'" should {
    "return correct values for the 'receiver' configuration section" in {
      settings.api.port       shouldBe 8080
    }
  }
}
