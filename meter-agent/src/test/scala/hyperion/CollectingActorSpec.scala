package hyperion

import akka.testkit.TestProbe

import scala.concurrent.duration._

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
      val receiver = TestProbe()
      val actor = system.actorOf(CollectingActor.props(receiver.ref), "emit-telegrams")

      val data = Seq(
        "/ISk5\\2MT382-1000",
        "",
        "1-3:0.2.8(40)",
        " 0-0:1.0.0(101209113020W)",
        "0-0:96.1.1(4B384547303034303436333935353037)",
        "1-0:1.8.1(123456.789*kWh)",
        "1-0:1.8.2(123456.789*kWh)",
        " 1-0:2.8.1(123456.789*kWh)",
        " 1-0:2.8.2(123456.789*kWh)",
        " 0-0:96.14.0(0002)",
        " 1-0:1.7.0(01.193*kW) ",
        "1-0:2.7.0(00.000*kW) ",
        "0-0:17.0.0(016.1*kW)",
        " 0-0:96.3.10(1) ",
        "0-0:96.7.21(00004)",
        " 0-0:96.7.9(00002)",
        "1-0:99:97.0(2)(0:96.7.19)(101208152415W)(0000000240*s)(101208151004W)(00000000301*s)",
        "1-0:32.32.0(00002)",
        "1-0:52.32.0(00001)",
        " 1-0:72:32.0(00000)",
        " 1-0:32.36.0(00000)",
        " 1-0:52.36.0(00003)",
        " 1-0:72.36.0(00000)",
        " 0-0:96.13.1(3031203631203831)",
        "0-0:96.13.0(303132333435363738393A3B3C3D3E3F303132333435363738393A3B3C3D3E3F303132333435363738393A3B 3C3D3E3F303132333435363738393A3B3C3D3E3F303132333435363738393A3B3C3D3E3F)",
        "0-1:24.1.0(03)",
        "0-1:96.1.0(3232323241424344313233343536373839)",
        "0-1:24.2.1(101209110000W)(12785.123*m3)",
        "0-1:24.4.0(1)",
        "!522B"
      )

      actor ! MeterAgent.IncomingData("!XXXX" + CRLF) // simulate end of previous message
      for (line <- data) {
        actor ! MeterAgent.IncomingData(line + CRLF)
      }
//      actor ! MeterAgent.IncomingData("garbage2" + CRLF)

      val lines = receiver.expectMsgPF(1000 milliseconds) {
        case CollectingActor.TelegramReceived(content) => content
      }
      lines(0) should ===("/ISk5\\2MT382-1000")
      lines(1) should ===("")
      lines(2) should ===("1-3:0.2.8(40)")
      lines(3) should ===(" 0-0:1.0.0(101209113020W)")
      lines(29) should ===("!522B")
    }
  }
}
