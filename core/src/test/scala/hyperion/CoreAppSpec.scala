package hyperion

import akka.testkit.TestProbe

import scala.concurrent.duration.DurationInt

class CoreAppSpec extends BaseAkkaSpec {

  "Creating the CoreApp" should {
    "result in creating the necessary top-level actors" in {
      // Act
      new CoreApp(system)

      // Assert
      TestProbe().expectActor("/user/launcher-actor", 500 milliseconds) should not be empty
    }
  }
}
