package hyperion

import akka.testkit.TestProbe
import hyperion.MessageDistributor.RegisterReceiver
import scala.concurrent.duration.DurationInt

class MessageDistributorSpec extends BaseAkkaSpec {
  private val md = system.actorOf(MessageDistributor.props(), "message-distributor")

  "The MessageDistributor Actor" should {
    "forward messages it receives to all recipients" in {
      // Arrange
      val msg = "msg"
      val probe = TestProbe()

      // Act
      probe.send(md, RegisterReceiver)
      md ! msg

      // Assert
      probe.expectMsgAllOf(msg)
    }

    "stop forwarding messages once a recipient is terminated" in {
      // Arrange
      val msg = "msg"
      val probe = TestProbe()

      // Act
      probe.send(md, RegisterReceiver)
      system.stop(probe.ref)
      Thread.sleep(500)
      md ! msg

      // Assert
      probe.expectNoMsg(1 second)
    }
  }
}
