package hyperion

import akka.testkit.TestProbe

import scala.concurrent.duration._
import scala.io.Source

import hyperion.p1.TelegramReceived

class CollectingActorSpec extends BaseAkkaSpec {
  private val newline = "\r\n"
  private val receiver = TestProbe()

  private val ca = system.actorOf(CollectingActor.props(receiver.ref), "collecting-actor")

  "Receiving the \"IncomingData\"" should {
    "skip data until the first complete Telegram comes in" in {
      ca ! MeterAgent.IncomingData(s"1-3:0.2.8(42)$newline!522B$newline/TEST")

      receiver.expectNoMessage(500 milliseconds)
    }

    "emit only complete Telegrams" in {
      //Arrange
      val source = Source.fromInputStream(getClass.getResourceAsStream("/valid-telegram1.txt"))
      val text = try source.mkString finally source.close()

      val data = text.grouped(100)

      // Act
      ca ! MeterAgent.IncomingData(s"$newline!XXXX$newline") // simulate end of previous message
      for (chunk <- data) {
        ca ! MeterAgent.IncomingData(chunk)
      }

      // Assert
      val telegram = receiver.expectMsgPF(1500 millis) {
        case TelegramReceived(content) => content
      }
      telegram.checksum shouldBe "522B" // other parsing is tested in P1TelegramParserSpec
    }
  }
}
