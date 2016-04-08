package hyperion

import akka.actor.ActorDSL.actor
import akka.testkit.TestProbe
import scala.concurrent.duration.DurationInt

class LauncherActorSpec extends BaseAkkaSpec {
  "Starting the Launcher Actor" should {
    "start the necessary child actors" in {
      // Arrange

      // Act
      val launcher = actor("create-children")(new LauncherActor(8080) {
        override protected def http() = TestProbe().ref
      })

      // Assert
      TestProbe().expectActor("/user/incoming-http-actor", 500 millis)
      TestProbe().expectActor("/user/receiver", 500 millis)
      TestProbe().expectActor("/user/recent-history", 500 millis)
    }
  }
}
