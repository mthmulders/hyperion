package hyperion

import akka.testkit.TestProbe

import scala.concurrent.duration._

class CollectingActorSpec extends BaseAkkaSpec {
  "Receiving the \"IncomingData\"" should {
    "skip data until the first complete Telegram comes in" in {
      val receiver = TestProbe()
      val actor = system.actorOf(CollectingActor.props(receiver.ref), "skip-data")
      val data = "1-3:0.2.8(42)\r\n!522B\r\n/TEST"

      actor ! MeterAgent.IncomingData(data)
      actor ! MeterAgent.IncomingData("\r\nbla\r\n!522B")
      actor ! MeterAgent.IncomingData("\r\n")

      receiver.expectMsgPF(100 milliseconds) {
        case CollectingActor.TelegramReceived(lines) =>
          lines.head should ===("/TEST")
          lines.head should ===("")
          lines.head should ===("bla")
          lines.head should ===("!522B")
      }
    }
  }
}
