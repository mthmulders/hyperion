package hyperion.p1

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, OffsetDateTime, ZoneId}

import scala.collection.immutable
import scala.util.Try
import scala.util.control.NoStackTrace
import scala.util.parsing.combinator.RegexParsers

import P1Constants._

final case class TelegramParseException private (message: String) extends Exception(message) with NoStackTrace

object P1TelegramParser extends RegexParsers {
  override def skipWhitespace = true

  private val dateFormat = DateTimeFormatter.ofPattern("yyMMddHHmmss")
  private val currentTimeZone = ZoneId.systemDefault()
  def asTimestamp(input: String): OffsetDateTime = {
    val localDateTime = LocalDateTime.parse(input, dateFormat)
    localDateTime.atOffset(currentTimeZone.getRules.getOffset(localDateTime))
  }
  def asBigDecimal(input: String): BigDecimal = BigDecimal(input)
  def asInt(input: String): Int = input.toInt

  private[this] val MAKE                 = "/" ~> "[A-Za-z0-9]{3}".r
  private[this] val IDENTIFICATION       = "5" ~> ".*".r
  private[this] val HEADER = MAKE ~ IDENTIFICATION ^^ {
    case make ~ identification => P1Header(make, identification)
  }

  private[this] val VERSION_INFO          = "1-3:0.2.8("   ~> """\d*""".r       <~ ")"
  private[this] val TELEGRAM_TIMESTAMP    = "0-0:1.0.0("   ~> """\d{12}""".r    <~ """[SW]\)""".r ^^ { asTimestamp }
  private[this] val EQUIPMENT_IDENTIFIER  = "0-0:96.1.1("  ~> """\w*""".r       <~ ")"
  private[this] val METADATA = VERSION_INFO ~ TELEGRAM_TIMESTAMP ~ EQUIPMENT_IDENTIFIER ^^ {
    case versionInfo ~ telegramTimestamp ~ equipmentIdentifier =>
      P1MetaData(versionInfo, telegramTimestamp, equipmentIdentifier)
  }

  private[this] val TARIFF_INDICATOR       = "0-0:96.14.0(" ~> """\d{4}""".r     <~ ")"
  private[this] val ELEC_CONS_1            = "1-0:1.8.1("   ~> """\d*\.?\d*""".r <~ "*kWh)"        ^^ { asBigDecimal }
  private[this] val ELEC_CONS_2            = "1-0:1.8.2("   ~> """\d*\.?\d*""".r <~ "*kWh)"        ^^ { asBigDecimal }
  private[this] val ELEC_PROD_1            = "1-0:2.8.1("   ~> """\d*\.?\d*""".r <~ "*kWh)"        ^^ { asBigDecimal }
  private[this] val ELEC_PROD_2            = "1-0:2.8.2("   ~> """\d*\.?\d*""".r <~ "*kWh)"        ^^ { asBigDecimal }
  private[this] val CURRENT_CONS           = "1-0:1.7.0("   ~> """\d*\.?\d*""".r <~ "*kW)"         ^^ { asBigDecimal }
  private[this] val CURRENT_PROD           = "1-0:2.7.0("   ~> """\d*\.?\d*""".r <~ "*kW)"         ^^ { asBigDecimal }

  private[this] val EXTERNAL_DEVICE_ID                    = "0-"                     ~> "[1-4]".r         <~ """:24.1.0\(\d{2,3}\)""".r ^^ { asInt }
  private[this] val EXTERNAL_DEVICE_EQUIPMENT_ID          = """0-[1-4]:96.1.0\(""".r ~> """\w{0,96}""".r    <~ ")"
  private[this] val EXTERNAL_DEVICE_GAS_READING_TIMESTAMP = """0-[1-4]:24.2.1\(""".r ~> """\d{12}""".r    <~ """[SW]\)""".r             ^^ { asTimestamp }
  private[this] val EXTERNAL_DEVICE_GAS_READING_VALUE     = "("                      ~> """\d*\.?\d*""".r <~ "*m3)"                     ^^ { asBigDecimal }
  private[this] val EXTERNAL_DEVICE_GAS_VALVE_POSITION    = """0-[1-4]:24.4.0\(""".r ~> """\d{1}""".r     <~ ")"                   ^^ { asInt }
  private[this] val GAS_METER = EXTERNAL_DEVICE_ID ~ EXTERNAL_DEVICE_EQUIPMENT_ID ~ EXTERNAL_DEVICE_GAS_READING_TIMESTAMP ~
    EXTERNAL_DEVICE_GAS_READING_VALUE ~ EXTERNAL_DEVICE_GAS_VALVE_POSITION.? ^^ {
    case deviceId ~ equipmentId ~ timestamp ~ reading ~ _ => P1GasMeter(deviceId, equipmentId, timestamp, reading)
  }

  private[this] val DATA = ELEC_CONS_1 ~ ELEC_CONS_2 ~ ELEC_PROD_1 ~ ELEC_PROD_2 ~ TARIFF_INDICATOR ~ CURRENT_CONS ~ CURRENT_PROD ~ IGNORED ~ GAS_METER.? ^^ {
    case elecCons1 ~ elecCons2 ~ elecProd1 ~ elecProd2 ~ tariffIndicator ~ currentCons ~ currentProd ~ _ ~ gasMeter =>

      val totalConsumption = immutable.Map(lowTariff -> elecCons1, normalTariff -> elecCons2)
      val totalProduction = immutable.Map(lowTariff -> elecProd1, normalTariff -> elecProd2)
      val devices = gasMeter.map(immutable.Seq(_)).getOrElse(immutable.Seq.empty[P1ExtraDevice])

      P1Data(tariffIndicator, currentCons, currentProd, totalConsumption, totalProduction, devices)
  }

  private[this] val CHECKSUM = "!" ~> """[A-Za-z0-9]{4}""".r
  private[this] val IGNORED =
    """0-0:17.0.0\(\d*\.?\d*\*kW\)""".r.?                                       ~ // The actual thresh- old Electricity in kW
    """0-0:96.3.10\(\d\)""".r.?                                                 ~ // Switch position Electricity (in/out/enabled)
    """0-0:96.7.21\(\d{5}\)""".r                                                ~ // Number of power failures in any phase
    """0-0:96.7.9\(\d{5}\)""".r                                                 ~ // Number of long power failures in any phase
    """1-0:99.97.0\(\d\)\(0-0:96.7.19\)(?:\(\d{12}[SW]\)\(\d{10,11}\*s\))+""".r ~ // Power Failure Event Log (long power failures)
                                                                                  // First param is number of repetitions of second & third param
    """1-0:32.32.0\(\d{5}\)""".r                                                ~ // Number of voltage sags in phase L1
    """1-0:52.32.0\(\d{5}\)""".r.?                                              ~ // Number of voltage sags in phase L2 (polyphase meters only)
    """1-0:72:32.0\(\d{5}\)""".r.?                                              ~ // Number of voltage sags in phase L3 (polyphase meters only)
    """1-0:32.36.0\(\d{5}\)""".r                                                ~ // Number of voltage swells in phase L1
    """1-0:52.36.0\(\d{5}\)""".r.?                                              ~ // Number of voltage swells in phase L2 (polyphase meters only)
    """1-0:72.36.0\(\d{5}\)""".r.?                                              ~ // Number of voltage swells in phase L3 (polyphase meters only)
    """0-0:96.13.1\([0-9a-fA-F]{0,16}\)""".r                                    ~ // Text message codes: numeric 8 digits, hex encoded
    """0-0:96.13.0\([0-9a-fA-F]{0,2048}\)""".r                                  ~ // Text message max 1024 characters, hex encoded
    // Below OBIS references are not present in version 4.0, but they are present in 4.2
    """1-0:31.7.0\(\d{3}\*A\)""".r.?                                            ~ // Instantaneous current L1 in A resolution
    """1-0:51.7.0\(\d{3}\*A\)""".r.?                                            ~ // Instantaneous current L2 in A resolution
    """1-0:71.7.0\(\d{3}\*A\)""".r.?                                            ~ // Instantaneous current L3 in A resolution
    """1-0:21.7.0\(\d*\.?\d*\*kW\)""".r.?                                       ~ // Instantaneous active power L1 (+P) in W resolution
    """1-0:41.7.0\(\d*\.?\d*\*kW\)""".r.?                                       ~ // Instantaneous active power L2 (+P) in W resolution
    """1-0:61.7.0\(\d*\.?\d*\*kW\)""".r.?                                       ~ // Instantaneous active power L3 (+P) in W resolution
    """1-0:22.7.0\(\d*\.?\d*\*kW\)""".r.?                                         // Instantaneous active power L1 (-P) in W resolution

  private[this] val TELEGRAM = HEADER ~ METADATA ~ DATA ~ CHECKSUM
  private[this] val parser: Parser[P1Telegram] = TELEGRAM ^^ {
    case header ~ metadata ~ data ~ checksum => P1Telegram(header, metadata, data, checksum)
  } | failure("Not all required lines are found")

  def parseTelegram(text: String): Try[P1Telegram] = parseAll(parser, text) match {
    case Success(result, _) => scala.util.Success(result)
    case NoSuccess(msg, _)  => scala.util.Failure(TelegramParseException(msg))
  }
}
