package hyperion

import akka.testkit.TestProbe

import scala.concurrent.duration.DurationInt

class HyperionAppSpec extends BaseAkkaSpec {

  "Creating the HyperionApp" should {
    "result in creating the necessary top-level actors" in {
      // Act
      new HyperionApp(system)

      // Assert
      TestProbe().expectActor("/user/launcher-actor", 4 seconds) should have size 1
    }
  }
}
