package hyperion

import java.time.{LocalDateTime, OffsetDateTime, ZoneId}

import org.scalatest.{OptionValues, Inside}

import scala.io.Source

import P1TelegramParser._
import P1Constants._

class P1TelegramParserSpec extends BaseSpec with Inside with OptionValues {

  val newline = "\r\n"

  private def localDateTimeAtCurrentOffset(ts: LocalDateTime): OffsetDateTime = {
    ts.atOffset(ZoneId.systemDefault().getRules.getOffset(ts))
  }

  "P1TelegramParser" should {
    "parse timestamps" in {
      asTimestamp("101209113020") shouldBe localDateTimeAtCurrentOffset(LocalDateTime.parse("2010-12-09T11:30:20"))
    }

    "parse decimals without decimal positions" in {
      asBigDecimal("3") shouldBe BigDecimal(3)
    }

    "parse decimals with decimal positions" in {
      asBigDecimal("3.14") shouldBe BigDecimal(3.14)
    }

    "parse integers" in {
      asInt("5") shouldBe 5
    }

    "parse strings" in {
      asString("Hello") shouldBe "Hello"
    }

    "parse voids" in {
      asNone("Hello") shouldBe None
    }

    "parse a complete telegram" in {
      val source = Source.fromInputStream(getClass.getResourceAsStream("/valid-telegram1.txt"))
      val text = try source.mkString finally source.close()

      val result: Option[P1Telegram] = parseTelegram(text)

      result shouldBe defined

      val gasTs = localDateTimeAtCurrentOffset(LocalDateTime.parse("2010-12-09T11:00:00"))
      val ts = localDateTimeAtCurrentOffset(LocalDateTime.parse("2010-12-09T11:30:20"))

      inside(result.get) {
        case P1Telegram(header, metadata, data, checksum) =>
          inside(header) { case P1Header(make, identification) =>
            make shouldBe "ISk"
            identification shouldBe "\\2MT382-1000"
          }
          inside(metadata) { case P1MetaData(versionInfo, timestamp, equipmentIdentifier) =>
            versionInfo shouldBe "40"
            equipmentIdentifier shouldBe "4B384547303034303436333935353037"
            timestamp shouldBe ts
          }
          inside(data) { case P1Data(currentTariff, currentConsumption, currentProduction, totalConsumption, totalProduction, devices) =>
            currentTariff shouldBe "0002"
            currentConsumption shouldBe 1.193
            currentProduction shouldBe 0

            totalConsumption should contain(lowTariff -> 123456.789)
            totalConsumption should contain(normalTariff -> 123456.789)

            totalProduction should contain(lowTariff -> 123456.789)
            totalProduction should contain(normalTariff -> 123456.789)

            devices should contain (P1GasMeter(1, "3232323241424344313233343536373839", gasTs, BigDecimal(12785.123)))
          }
          checksum shouldBe "522B"
      }
    }

    "parse another complete telegram" in {
      val source = Source.fromInputStream(getClass.getResourceAsStream("/valid-telegram2.txt"))
      val text = try source.mkString finally source.close()

      val result: Option[P1Telegram] = parseTelegram(text)

      result shouldBe defined
    }

    "not parse a malformed telegram" in {
      parseTelegram("foo") should not be defined
    }
  }

}
