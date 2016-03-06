package hyperion

import akka.testkit.EventFilter

class ReceiverActorSpec extends BaseAkkaSpec {
  "The Receiver Actor" should {
    "log incoming messages" in {
      // Arrange
      val receiver = system.actorOf(ReceiverActor.props(), "log-messages")
      val msg = "msg"

      // Act & Assert
      EventFilter.debug(message = s"Got message [$msg]", occurrences = 1) intercept {
        receiver ! msg
      }
    }
  }
}
