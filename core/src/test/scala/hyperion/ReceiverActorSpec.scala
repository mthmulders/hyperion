package hyperion

import akka.testkit.{EventFilter, TestProbe}
import hyperion.ReceiverActor.RegisterReceiver

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

    "forward messages it receives to all recipients" in {
      // Arrange
      val receiver = system.actorOf(ReceiverActor.props(), "forward-messages")
      val msg = "msg"
      val probe = TestProbe()

      // Act
      probe.send(receiver, RegisterReceiver)
      receiver ! msg

      // Assert
      probe.expectMsgAllOf(msg)
    }
  }
}
