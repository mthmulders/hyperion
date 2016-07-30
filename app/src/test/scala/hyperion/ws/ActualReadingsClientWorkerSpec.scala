package hyperion.ws

import akka.actor.ActorDSL.actor
import akka.actor.Props
import akka.testkit.TestProbe
import hyperion.MessageDistributor.RegisterReceiver
import hyperion._
import hyperion.rest.{HyperionJsonProtocol, MeterReading}
import org.scalatest.OptionValues
import spray.can.server.UHttp.UpgradeServer
import spray.can.websocket
import spray.can.websocket.FrameCommand
import spray.can.websocket.frame.TextFrame
import spray.http.{HttpHeader, StatusCodes}
import spray.json._

class ActualReadingsClientWorkerSpec extends BaseAkkaSpec with OptionValues with HyperionJsonProtocol {
  "Initially" should {
    "register itself with the Message Distributor" in {
      // Arrange
      val messageDistributor = TestProbe("receiver")

      // Act
      system.actorOf(Props(new ActualReadingsClientWorker(messageDistributor.ref, TestProbe().ref)), "register")

      // Assert
      messageDistributor.expectMsg(RegisterReceiver)
    }

    "upgrade connection to WebSocket" in {
      // Arrange
      val client = TestProbe()

      // Act
      val sut = actor("upgrade-connection")(new ActualValuesHandlerActor(client.ref, TestProbe().ref))
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
      val sut = actor("send-new-reading")(new ActualValuesHandlerActor(client.ref, TestProbe().ref))
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
