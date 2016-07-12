package hyperion

import akka.actor.ActorDSL.actor
import akka.testkit.TestProbe
import scala.concurrent.duration.DurationInt

class LauncherActorSpec extends BaseAkkaSpec {
  "Starting the Launcher Actor" should {
    "start the necessary child actors" in {
      // Arrange

      // Act
      val launcher = actor("create-children")(new LauncherActor() {
        override protected def http() = TestProbe().ref
      })
      val createdActors = TestProbe().expectActor("/user/*", 4 seconds).filterNot(_ == launcher)
      val createdPaths = createdActors.map(_.path.toString).map(_.replace("akka://default/user/", ""))

      // Assert
      createdActors should have size 6
      createdPaths should contain allOf ("receiver", "incoming-http-actor", "recent-history", "collecting-actor",
        "meter-agent", "daily-history")
    }
  }
}
