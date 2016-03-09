package hyperion

import akka.testkit.TestProbe

import scala.concurrent.duration.DurationInt

class CoreAppSpec extends BaseAkkaSpec {
  val app = new CoreApp(system)

  "Creating the CoreApp" should {
    "result in creating the necessary top-level actors" in {
      // Assert
      TestProbe().expectActor("/user/receiver", 500 milliseconds) should not be empty
    }
  }
}
