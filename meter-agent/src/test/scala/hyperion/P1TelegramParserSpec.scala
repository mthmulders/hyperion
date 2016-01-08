package hyperion

import org.scalatest.Inside

class P1TelegramParserSpec extends BaseSpec with Inside {
  import P1TelegramParser._

  val CRLF = "\r\n"

  "P1TelegramParser" should {
    "parse a complete telegram" in {
      val text = "/ISk5\\2MT382-1000" + CRLF +
        "" + CRLF +
        "1-3:0.2.8(40)" + CRLF +
        "0-0:1.0.0(101209113020W)" + CRLF +
        "0-0:96.1.1(4B384547303034303436333935353037)" + CRLF +
        "1-0:1.8.1(123456.789*kWh)" + CRLF +
        "1-0:1.8.2(123456.789*kWh)" + CRLF +
        "1-0:2.8.1(123456.789*kWh)" + CRLF +
        "1-0:2.8.2(123456.789*kWh)" + CRLF +
        "0-0:96.14.0(0002)" + CRLF +
        "1-0:1.7.0(01.193*kW)" + CRLF +
        "1-0:2.7.0(00.000*kW)" + CRLF +
        "0-0:17.0.0(016.1*kW)" + CRLF +
        "0-0:96.3.10(1)" + CRLF +
        "0-0:96.7.21(00004)" + CRLF +
        "0-0:96.7.9(00002)" + CRLF +
        "1-0:99:97.0(2)(0:96.7.19)(101208152415W)(0000000240*s)(101208151004W)(00000000301*s)" + CRLF +
        "1-0:32.32.0(00002)" + CRLF +
        "1-0:52.32.0(00001)" + CRLF +
        "1-0:72:32.0(00000)" + CRLF +
        "1-0:32.36.0(00000)" + CRLF +
        "1-0:52.36.0(00003)" + CRLF +
        "1-0:72.36.0(00000)" + CRLF +
        "0-0:96.13.1(3031203631203831)" + CRLF +
        "0-0:96.13.0(303132333435363738393A3B3C3D3E3F303132333435363738393A3B3C3D3E3F303132333435363738393A3B3C3D3E3F303132333435363738393A3B3C3D3E3F303132333435363738393A3B3C3D3E3F)" + CRLF +
        "0-1:24.1.0(03)" + CRLF +
        "0-1:96.1.0(3232323241424344313233343536373839)" + CRLF +
        "0-1:24.2.1(101209110000W)(12785.123*m3)" + CRLF +
        "0-1:24.4.0(1)" + CRLF +
        "!522B" + CRLF

      val result: Option[P1Telegram] = parse(text)

      result shouldBe defined

      inside(result.get) {
        case P1Telegram(header, metadata, data, checksum) =>
          inside(header) { case P1Header(make, identification) =>
            make shouldBe "ISk"
            identification shouldBe "\\2MT382-1000"
          }
          inside(metadata) { case P1MetaData(versionInfo, timestamp, equipmentIdentifier) =>
            versionInfo shouldBe "40"
            equipmentIdentifier shouldBe "4B384547303034303436333935353037"
          }
          inside(data) { case P1Data(currentTariff, currentConsumption, currentProduction, totalConsumption, totalProduction, devices) =>
            currentTariff shouldBe "0002"
            currentConsumption shouldBe 1.193
            currentProduction shouldBe 0

            totalConsumption should not be null
            totalConsumption("1") shouldBe 123456.789
            totalConsumption("2") shouldBe 123456.789

            totalProduction should not be null
            totalProduction("1") shouldBe 123456.789
            totalProduction("2") shouldBe 123456.789
          }
          inside(checksum) { case P1Checksum(value) =>
            value shouldBe "522B"
          }
      }
    }

    "not parse a malformed telegram" in {
      parse("foo") should not be defined
    }
  }

}
