package hyperion

import akka.testkit.TestProbe

import scala.concurrent.duration._
import scala.io.Source

class CollectingActorSpec extends BaseAkkaSpec {
  val CRLF = "\r\n"

  "Receiving the \"IncomingData\"" should {
    "skip data until the first complete Telegram comes in" in {
      val receiver = TestProbe()
      val actor = system.actorOf(CollectingActor.props(receiver.ref), "skip-data")

      actor ! MeterAgent.IncomingData("1-3:0.2.8(42)\r\n!522B\r\n/TEST")

      receiver.expectNoMsg(500 milliseconds)
    }

    "emit only complete Telegrams" in {
      //Arrange
      val receiver = TestProbe()
      val actor = system.actorOf(CollectingActor.props(receiver.ref), "emit-telegrams")

      val source = Source.fromInputStream(getClass.getResourceAsStream("/valid-telegram1.txt"))
      val text = try source.mkString finally source.close()

      val data = text.grouped(100).toIndexedSeq

      // Act
      actor ! MeterAgent.IncomingData("!XXXX" + CRLF) // simulate end of previous message
      for (chunk <- data) {
        actor ! MeterAgent.IncomingData(chunk)
      }

      // Assert
      val telegram = receiver.expectMsgPF(1 second) {
        case TelegramReceived(content) => content
      }
      telegram.checksum shouldBe "522B" // other parsing is tested in P1TelegramParserSpec
    }
  }
}
