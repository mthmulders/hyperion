package hyperion

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, OffsetDateTime, ZoneId}

import org.slf4j.LoggerFactory

import scala.collection.immutable
import util.parsing.combinator.RegexParsers

import P1Constants._

object P1TelegramParser extends RegexParsers {
  private val logger = LoggerFactory.getLogger(getClass)

  override def skipWhitespace = true

  private val dateFormat = DateTimeFormatter.ofPattern("yyMMddHHmmss")
  private val currentTimeZone = ZoneId.systemDefault()
  def asTimestamp(input: String): OffsetDateTime = {
    val localDateTime = LocalDateTime.parse(input, dateFormat)
    localDateTime.atOffset(currentTimeZone.getRules.getOffset(localDateTime))
  }
  def asBigDecimal(input: String): BigDecimal = BigDecimal(input)
  def asInt(input: String): Int = input.toInt
  def asString(input: String): String = input
  def asNone(input: String): Option[Nothing] = None

  private[this] val make                 = "/" ~> "[A-Za-z0-9]{3}".r ^^ { asString }
  private[this] val identification       = "5" ~> ".*".r             ^^ { asString }
  private[this] val header = make ~ identification ^^ {
    case make ~ identification => P1Header(make, identification)
  }

  private[this] val versionInfo          = "1-3:0.2.8("   ~> """\d*""".r       <~ ")"            ^^ { asString }
  private[this] val telegramTimestamp    = "0-0:1.0.0("   ~> """\d{12}""".r    <~ """[SW]\)""".r ^^ { asTimestamp }
  private[this] val equipmentIdentifier  = "0-0:96.1.1("  ~> """\w*""".r       <~ ")"            ^^ { asString }
  private[this] val metadata = versionInfo ~ telegramTimestamp ~ equipmentIdentifier ^^ {
    case versionInfo ~ telegramTimestamp ~ equipmentIdentifier =>
      P1MetaData(versionInfo, telegramTimestamp, equipmentIdentifier)
  }

  private[this] val tariffIndicator      = "0-0:96.14.0(" ~> """\d{4}""".r     <~ ")"            ^^ { asString }
  private[this] val elecCons1            = "1-0:1.8.1("   ~> """\d*\.?\d*""".r <~ "*kWh)"        ^^ { asBigDecimal }
  private[this] val elecCons2            = "1-0:1.8.2("   ~> """\d*\.?\d*""".r <~ "*kWh)"        ^^ { asBigDecimal }
  private[this] val elecProd1            = "1-0:2.8.1("   ~> """\d*\.?\d*""".r <~ "*kWh)"        ^^ { asBigDecimal }
  private[this] val elecProd2            = "1-0:2.8.2("   ~> """\d*\.?\d*""".r <~ "*kWh)"        ^^ { asBigDecimal }
  private[this] val currentCons          = "1-0:1.7.0("   ~> """\d*\.?\d*""".r <~ "*kW)"         ^^ { asBigDecimal }
  private[this] val currentProd          = "1-0:2.7.0("   ~> """\d*\.?\d*""".r <~ "*kW)"         ^^ { asBigDecimal }

  private[this] val externalDeviceId                  = "0-"                     ~> "[1-4]".r         <~ """:24.1.0\(\d{2,3}\)""".r ^^ { asInt }
  private[this] val externalDeviceEquipmentId         = """0-[1-4]:96.1.0\(""".r ~> """\w{34}""".r    <~ ")"                   ^^ { asString }
  private[this] val externalDeviceGasReadingTimestamp = """0-[1-4]:24.2.1\(""".r ~> """\d{12}""".r    <~ """[SW]\)""".r             ^^ { asTimestamp }
  private[this] val externalDeviceGasReadingValue     = "("                      ~> """\d*\.?\d*""".r <~ "*m3)"                     ^^ { asBigDecimal }
  private[this] val externalDeviceGasValvePosition    = """0-[1-4]:24.4.0\(""".r ~> """\d{1}""".r     <~ ")"                   ^^ { asInt }
  private[this] val gasMeter = externalDeviceId ~ externalDeviceEquipmentId ~ externalDeviceGasReadingTimestamp ~
    externalDeviceGasReadingValue ~ externalDeviceGasValvePosition.? ^^ {
    case deviceId ~ equipmentId ~ timestamp ~ reading ~ _ => P1GasMeter(deviceId, equipmentId, timestamp, reading)
  }

  private[this] val data = elecCons1 ~ elecCons2 ~ elecProd1 ~ elecProd2 ~ tariffIndicator ~ currentCons ~ currentProd ~ ignored ~ gasMeter.? ^^ {
    case elecCons1 ~ elecCons2 ~ elecProd1 ~ elecProd2 ~ tariffIndicator ~ currentCons ~ currentProd ~ _ ~ gasMeter =>

      val totalConsumption = immutable.Map(lowTariff -> elecCons1, normalTariff -> elecCons2)
      val totalProduction = immutable.Map(lowTariff -> elecProd1, normalTariff -> elecProd2)
      val devices = gasMeter.map(immutable.Seq(_)).getOrElse(immutable.Seq.empty[P1ExtraDevice])

      P1Data(tariffIndicator, currentCons, currentProd, totalConsumption, totalProduction, devices)
  }

  private[this] val checksum = "!" ~> """[A-Za-z0-9]{4}""".r ^^ { asString }
  private[this] val ignored =
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

  private[this] val telegram = header ~ metadata ~ data ~ checksum
  private[this] val parser: Parser[P1Telegram] = telegram ^^ {
    case header ~ metadata ~ data ~ checksum => P1Telegram(header, metadata, data, checksum)
  } | failure("Not all required lines are found")


  def parseTelegram(text: String): Option[P1Telegram] = parseAll(parser, text) match {
    case Success(result, _) => Some(result)
    case Failure(msg, _)    => handleParseFailure(msg); None
    case Error(msg, _)      => handleParseFailure(msg); None
  }

  private[this] def handleParseFailure(msg: String) = {
    logger.error("Could not parse telegram: {}", msg)
  }
}
