package hyperion.rest

import java.time.LocalDateTime

import akka.actor.ActorDSL.actor
import akka.testkit.TestProbe
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
  val telegram = P1Telegram(
    P1Header("", ""),
    P1MetaData("", LocalDateTime.now(), ""),
    P1Data(P1Constants.LOW_TARIFF, BigDecimal(0L), BigDecimal(0L), Map.empty, Map.empty, None),
    P1Checksum("")
  )

  "Initially" should {
    "upgrade connection to WebSocket" in {
      // Arrange
      val client = TestProbe()

      // Act
      val sut = actor("upgrade-connection")(new ActualValuesRequestHandlingActor(client.ref))
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

      // Act
      val sut = actor("send-new-reading")(new ActualValuesRequestHandlingActor(client.ref))
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
