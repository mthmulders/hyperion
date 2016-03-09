package hyperion

import java.net.URL

import akka.testkit.TestProbe

class LauncherActorSpec extends BaseAkkaSpec {
  "Starting the Launcher Actor" should {
    "start the necessary child actors" in {
      // Arrange

      // Act
      val launcher = system.actorOf(LauncherActor.props(8080))

      // Assert
      TestProbe().expectActor("/user/incoming-http-actor")
      TestProbe().expectActor("/user/receiver")
      system.stop(launcher)
    }
  }
}
