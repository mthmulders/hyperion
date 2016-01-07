package hyperion

import java.time.{Month, LocalDateTime}

import org.scalatest.Inside

import scala.util.parsing.input.CharSequenceReader

class P1TelegramParsersSpec extends BaseSpec with P1TelegramParsers with Inside {
  import P1RecordType._

  val CRLF = "\r\n"

  "P1TelegramParser" should {
    "parse a complete telegram" in {
      implicit val parserToTest = p1parser

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

      val result = parsing(text)

      inside(result) {
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
  }

  "P1TelegramParser components" should {
    "parse meter make and identification from header" in {
      implicit val parserToTest = parsers.header

      parsing("/KFM5KAIFA-METER") should ===(P1Header("KFM", "KAIFA-METER"))
      parsing("/ISk5\\2MT382-1000") should ===(P1Header("ISk", "\\2MT382-1000"))

      intercept[IllegalArgumentException] {
        parsing("bummer")
      }
    }

    "parse the checksum" in {
      implicit val parserToTest = parsers.checksum

      parsing("!522B") should ===(P1Checksum("522B"))
      parsing("!3A6C") should ===(P1Checksum("3A6C"))

      intercept[IllegalArgumentException] {
        parsing("bummmer")
      }
    }

    "parse version information data record" in {
      implicit val parserToTest = parsers.versionInfo

      val result = parsing("1-3:0.2.8(40)")
      result shouldBe defined
      result shouldBe Some(P1Record(VERSION_INFORMATION, "40"))
    }

    "parse date time stamp data record" in {
      implicit val parserToTest = parsers.dateTimeStamp

      val expectedDate = LocalDateTime.of(2010, Month.DECEMBER, 9, 11, 30, 20)
      var result = parsing("0-0:1.0.0(101209113020W)")
      result shouldBe defined
      result shouldBe Some(P1Record(DATE_TIME_STAMP, expectedDate))

      result = parsing("0-0:1.0.0(101209113020S)")
      result shouldBe defined
      result shouldBe Some(P1Record(DATE_TIME_STAMP, expectedDate))
    }

    "parse equipment identifier data record" in {
      implicit val parserToTest = parsers.equipmentId

      val result = parsing("0-0:96.1.1(4B384547303034303436333935353037)")
      result shouldBe defined
      result shouldBe Some(P1Record(EQUIPMENT_IDENTIFIER, "4B384547303034303436333935353037"))
    }

    "parse electricity consumed in tariff 1 data record" in {
      implicit val parserToTest = parsers.elecConsTariff1

      val result = parsing("1-0:1.8.1(10435.217*kWh)")
      result shouldBe defined
      result shouldBe Some(P1Record(ELECTRICITY_CONSUMED_TARIFF_1, 10435.217))
    }

    "parse electricity consumed in tariff 2 data record" in {
      implicit val parserToTest = parsers.elecConsTariff2

      val result = parsing("1-0:1.8.2(10317.329*kWh)")
      result shouldBe defined
      result shouldBe Some(P1Record(ELECTRICITY_CONSUMED_TARIFF_2, 10317.329))
    }

    "parse electricity produced in tariff 1 data record" in {
      implicit val parserToTest = parsers.elecProdTariff1

      val result = parsing("1-0:2.8.1(00000.003*kWh)")
      result shouldBe defined
      result shouldBe Some(P1Record(ELECTRICITY_PRODUCED_TARIFF_1, 0.003))
    }

    "parse electricity produced in tariff 2 data record" in {
      implicit val parserToTest = parsers.elecProdTariff2

      val result = parsing("1-0:2.8.2(00000.001*kWh)")
      result shouldBe defined
      result shouldBe Some(P1Record(ELECTRICITY_PRODUCED_TARIFF_2, 0.001))
    }

    "parse tariff indicator data record" in {
      implicit val parserToTest = parsers.tariffIndicator

      val result = parsing("0-0:96.14.0(0001)")
      result shouldBe defined
      result shouldBe Some(P1Record(TARIFF_INDICATOR, "0001"))
    }

    "parse current consumption data record" in {
      implicit val parserToTest = parsers.currentCons

      val result = parsing("1-0:1.7.0(0000.98*kW)")
      result shouldBe defined
      result shouldBe Some(P1Record(CURRENT_CONSUMPTION, 0.98))
    }

    "parse current production data record" in {
      implicit val parserToTest = parsers.currentProd

      val result = parsing("1-0:2.7.0(0000.75*kW)")
      result shouldBe defined
      result shouldBe Some(P1Record(CURRENT_PRODUCTION, 0.75))
    }

    "parse the device type of external device 1" in {
      implicit val parserToTest = parsers.deviceType

      val result = parsing("0-1:24.1.0(03)")
      result shouldBe defined
      result shouldBe Some(P1Record(EXTERNAL_DEVICE_TYPE, (1, "03")))
    }

    "parse the equipment identifier of external device 1" in {
      implicit val parserToTest = parsers.deviceEquipmentId

      val result = parsing("0-1:96.1.0(3232323241424344313233343536373839)")
      result shouldBe defined
      result shouldBe Some(P1Record(EXTERNAL_DEVICE_EQUIPMENT_ID, (1, "3232323241424344313233343536373839")))
    }

    "parse the last gas meter reading of external device 1" in {
      implicit val parserToTest = parsers.deviceGasReading

      val expectedDate = LocalDateTime.of(2010, Month.DECEMBER, 9, 11, 0, 0)
      var result = parsing("0-1:24.2.1(101209110000W)(12785.123*m3)")
      result shouldBe defined
      result shouldBe Some(P1Record(EXTERNAL_DEVICE_GAS_READING, (1, expectedDate, 12785.123)))

      result = parsing("0-1:24.2.1(101209110000S)(12785.123*m3)")
      result shouldBe defined
      result shouldBe Some(P1Record(EXTERNAL_DEVICE_GAS_READING, (1, expectedDate, 12785.123)))
    }

    "ignore values that are not relevant" in {
      implicit var parserToTest = parsers.ignored

      parsing("0-0:17.0.0(016.1*kW)") should not be defined
      parsing("0-0:96.3.10(1)") should not be defined
      parsing("0-0:96.7.21(00004)") should not be defined
      parsing("0-0:96.7.9(00002)") should not be defined
      parsing("1-0:99:97.0(2)(0:96.7.19)(101208152415W)(0000000240*s)(101208151004W)(00000000301*s)") should not be defined
      parsing("1-0:32.32.0(00002)") should not be defined
      parsing("1-0:52.32.0(00001)") should not be defined
      parsing("1-0:72:32.0(00000)") should not be defined
      parsing("1-0:32.36.0(00000)") should not be defined
      parsing("1-0:52.36.0(00003)") should not be defined
      parsing("1-0:72.36.0(00000)") should not be defined
      parsing("0-0:96.13.1(3031203631203831)") should not be defined
      parsing("0-0:96.13.0(303132333435363738393A3B3C3D3E3F303132333435363738393A3B3C3D3E3F303132333435363738393A3B3C3D3E3F303132333435363738393A3B3C3D3E3F303132333435363738393A3B3C3D3E3F)") should not be defined
    }
  }

  // Helper functions. Credits to Christoph Henkelmann
  // (http://henkelmann.eu/2011/01/29/an_introduction_to_scala_parser_combinators-part_3_unit_tests)
  private def parsing[T](s:String)(implicit p:Parser[T]):T = {
    //wrap the parser in the phrase parse to make sure all input is consumed
    val phraseParser = phrase(p)
    //we need to wrap the string in a reader so our parser can digest it
    val input = new CharSequenceReader(s)
    phraseParser(input) match {
      case Success(t, _)     => t
      case NoSuccess(msg, next) => {
        throw new IllegalArgumentException(s"Could not parse '$s' (next '$next'): " + msg)
      }
    }
  }

}
