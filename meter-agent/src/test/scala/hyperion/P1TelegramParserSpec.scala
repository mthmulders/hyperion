package hyperion

import org.scalatest.Inside

import scala.io.Source

import P1TelegramParser._
import P1Constants._

class P1TelegramParserSpec extends BaseSpec with Inside {

  val CRLF = "\r\n"

  "P1TelegramParser" should {
    "parse a complete telegram" in {
      val source = Source.fromInputStream(getClass.getResourceAsStream("/valid-telegram.txt"))
      val text = try source.mkString finally source.close()

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

            totalConsumption should contain(LOW_TARIFF -> 123456.789)
            totalConsumption should contain(NORMAL_TARIFF -> 123456.789)

            totalProduction should contain(LOW_TARIFF -> 123456.789)
            totalProduction should contain(NORMAL_TARIFF -> 123456.789)

            devices should be ('empty)
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
