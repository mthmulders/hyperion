package hyperion.ws

import scala.concurrent.duration.DurationInt

import akka.actor.Props
import akka.testkit.TestProbe

import hyperion.{BaseAkkaSpec, TestSupport}
import hyperion.MessageDistributor.RegisterReceiver

class ActualValuesHandlerActorSpec extends BaseAkkaSpec {
  private val messageDistributor = TestProbe()
  private val source = TestProbe()

  private val avha = system.actorOf(Props(new ActualValuesHandlerActor(source.ref, messageDistributor.ref)), "actual-values-handler")

  "The ActualReadings WebSocket worker" should {
    "should register with the message distributor" in {
      // Act
      system.actorOf(Props(new ActualValuesHandlerActor(source.ref, messageDistributor.ref)), "register")

      // Assert
      messageDistributor.expectMsg(RegisterReceiver)
    }

    "should push messages back to the WebSocket client" in {
      // Act
      val msg = TestSupport.randomTelegram()

      // Arrange
      messageDistributor.send(source.ref, msg)

      // Act
      within(500 millis) {
        source.expectMsg(msg)
      }
    }
  }
}
