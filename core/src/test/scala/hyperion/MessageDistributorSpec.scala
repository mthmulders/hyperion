package hyperion

import akka.testkit.TestProbe
import hyperion.MessageDistributor.RegisterReceiver
import scala.concurrent.duration.DurationInt

class MessageDistributorSpec extends BaseAkkaSpec {
  "The MessageDistributor Actor" should {
    "forward messages it receives to all recipients" in {
      // Arrange
      val receiver = system.actorOf(MessageDistributor.props(), "forward-messages")
      val msg = "msg"
      val probe = TestProbe()

      // Act
      probe.send(receiver, RegisterReceiver)
      receiver ! msg

      // Assert
      probe.expectMsgAllOf(msg)
    }

    "stop forwarding messages once a recipient is stopped" in {
      // Arrange
      val receiver = system.actorOf(MessageDistributor.props(), "stop-forwarding")
      val msg = "msg"
      val probe = TestProbe()

      // Act
      probe.send(receiver, RegisterReceiver)
      system.stop(probe.ref)
      Thread.sleep(100)
      receiver ! msg

      // Assert
      probe.expectNoMsg(250 millis)
    }
  }
}
