package hyperion

import akka.testkit.TestProbe

import scala.concurrent.duration._

class CollectingActorSpec extends BaseAkkaSpec {
  "Receiving the \"IncomingData\"" should {
    "skip data until the first complete Telegram comes in" in {
      val receiver = TestProbe()
      val actor = system.actorOf(CollectingActor.props(receiver.ref), "skip-data")

      actor ! MeterAgent.IncomingData("1-3:0.2.8(42)\r\n!522B\r\n/TEST")

      receiver.expectNoMsg(500 milliseconds)
    }

    "emit only complete Telegrams" in {
      val receiver = TestProbe()
      val actor = system.actorOf(CollectingActor.props(receiver.ref), "emit-telegrams")

      actor ! MeterAgent.IncomingData("/TEST")
      actor ! MeterAgent.IncomingData("\r\n\r\nbla\r\n!522B")
      actor ! MeterAgent.IncomingData("\r\n")

      val lines = receiver.expectMsgPF(100 milliseconds) {
        case CollectingActor.TelegramReceived(content) => content
      }
      lines(0) should ===("/TEST")
      lines(1) should ===("")
      lines(2) should ===("bla")
      lines(3) should ===("!522B")
    }
  }
}
