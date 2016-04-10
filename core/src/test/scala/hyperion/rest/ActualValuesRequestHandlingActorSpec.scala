package hyperion.rest

import akka.actor.ActorDSL.actor
import akka.testkit.TestProbe
import hyperion.MessageDistributor.RegisterReceiver
import hyperion._
import org.scalatest.OptionValues
import spray.can.server.UHttp.UpgradeServer
import spray.can.websocket
import spray.can.websocket.frame.TextFrame
import spray.can.websocket.FrameCommand
import spray.http.{HttpHeader, StatusCodes}
import spray.json._
import HyperionJsonProtocol._

class ActualValuesRequestHandlingActorSpec extends BaseAkkaSpec with OptionValues {
  "Initially" should {
    "register itself with the Message Distributor" in {
      // Arrange
      val messageDistributor = TestProbe("receiver")

      // Act
      system.actorOf(ActualValuesRequestHandlingActor.props(TestProbe().ref, messageDistributor.ref), "register")

      // Assert
      messageDistributor.expectMsg(RegisterReceiver)
    }

    "upgrade connection to WebSocket" in {
      // Arrange
      val client = TestProbe()

      // Act
      val sut = actor("upgrade-connection")(new ActualValuesRequestHandlingActor(client.ref, TestProbe().ref))
      client.send(sut, websocket.basicHandshakeRepuset("/actual"))

      // Assert
      val msg = client.expectMsgAllClassOf(classOf[UpgradeServer]).headOption
      msg shouldBe defined
      msg.value.response.status should be(StatusCodes.SwitchingProtocols)
      val headers: List[HttpHeader] = msg.value.response.headers
      headers.find(_.is("upgrade")).map(_.value).value should be("websocket")
      headers.find(_.is("connection")).map(_.value).value should be("Upgrade")
    }
  }

  "With upgraded connection" should {
    "send update to client when new telegram comes in" in {
      // Arrange
      val client = TestProbe()
      val telegram = TestSupport.randomTelegram()

      // Act
      val sut = actor("send-new-reading")(new ActualValuesRequestHandlingActor(client.ref, TestProbe().ref))
      client.send(sut, websocket.basicHandshakeRepuset("/actual"))
      sut ! TelegramReceived(telegram)

      // Assert
      client.expectMsgAnyClassOf(classOf[UpgradeServer]) // ignored for this test
      val msg = client.expectMsgAnyClassOf(classOf[FrameCommand]).frame.asInstanceOf[TextFrame].payload.utf8String
      val reading = msg.parseJson.convertTo[MeterReading]
      reading.ts should be(telegram.metadata.timestamp)
    }
  }
}
