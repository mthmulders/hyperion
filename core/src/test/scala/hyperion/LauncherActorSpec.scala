package hyperion

import akka.testkit.TestProbe
import scala.concurrent.duration.DurationInt

class LauncherActorSpec extends BaseAkkaSpec {
  "Starting the Launcher Actor" should {
    "start the necessary child actors" in {
      // Arrange

      // Act
      val launcher = system.actorOf(LauncherActor.props(8080))

      // Assert
      TestProbe().expectActor("/user/incoming-http-actor", 500 millis)
      TestProbe().expectActor("/user/receiver", 500 millis)
      system.stop(launcher)
    }
  }
}
