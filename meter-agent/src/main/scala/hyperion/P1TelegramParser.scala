package hyperion

import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
import java.util.Date

import org.slf4j.LoggerFactory

import scala.collection.{mutable, immutable}
import scala.reflect.ClassTag
import util.parsing.combinator.RegexParsers

import P1RecordType._

object P1TelegramParser extends RegexParsers {

  private val logger = LoggerFactory.getLogger("P1TelegramParser")

  private case class P1Record[T](recordType: P1RecordType, value: T)

  private val headerPattern = """\/([A-Za-z0-9]{3})5(.*)""".r
  private val header = headerPattern ^^ { case headerPattern(make, identification) => P1Header(make, identification)}

  private val checksumPattern = """!([A-Za-z0-9]{4})""".r
  private val checksum = checksumPattern ^^ { case checksumPattern(value) => P1Checksum(value) }

  private val dateFormat = DateTimeFormatter.ofPattern("yyMMddHHmmss")
  private class ParseAs(val s: String, recordType: P1RecordType) {
    def using[T: ClassTag](extractor: String => T): Parser[Option[P1Record[T]]] = {
      val compiledPattern = s.r
      compiledPattern ^^ {
        case compiledPattern(value) => Some(P1Record(recordType, extractor(value)))
      }
    }

    def using[T: ClassTag](extractor: (String, String) => T): Parser[Option[P1Record[T]]] = {
      val compiledPattern = s.r
      compiledPattern ^^ {
        case compiledPattern(value1, value2) => Some(P1Record(recordType, extractor(value1, value2)))
      }
    }

    def using[T: ClassTag](extractor: (String, String, String) => T): Parser[Option[P1Record[T]]] = {
      val compiledPattern = s.r
      compiledPattern ^^ {
        case compiledPattern(value1, value2, value3) => Some(P1Record(recordType, extractor(value1, value2, value3)))
      }
    }
  }

  private def extract[T:ClassTag](s: String)(recordType: P1RecordType) = new ParseAs(s, recordType)

  // Patterns and parsers for various P1 Records
  private val versionInfo       = extract("""1-3:0\.2\.8\((\d*)\)""")              (VERSION_INFORMATION)            using (s => s)
  private val dateTimeStamp     = extract("""0-0:1\.0\.0\((\d{12})[SW]\)""")       (DATE_TIME_STAMP)                using (s => LocalDateTime.parse(s, dateFormat))
  private val equipmentId       = extract("""0-0:96\.1\.1\((\w*)\)""")             (EQUIPMENT_IDENTIFIER)           using (s => s)
  private val elecConsTariff1   = extract("""1-0:1\.8\.1\((\d*\.?\d*)\*kWh\)""")   (ELECTRICITY_CONSUMED_TARIFF_1)  using (s => BigDecimal(s))
  private val elecConsTariff2   = extract("""1-0:1\.8\.2\((\d*\.?\d*)\*kWh\)""")   (ELECTRICITY_CONSUMED_TARIFF_2)  using (s => BigDecimal(s))
  private val elecProdTariff1   = extract("""1-0:2\.8\.1\((\d*\.?\d*)\*kWh\)""")   (ELECTRICITY_PRODUCED_TARIFF_1)  using (s => BigDecimal(s))
  private val elecProdTariff2   = extract("""1-0:2\.8\.2\((\d*\.?\d*)\*kWh\)""")   (ELECTRICITY_PRODUCED_TARIFF_2)  using (s => BigDecimal(s))
  private val tariffIndicator   = extract("""0-0:96\.14\.0\((\d{4})\)""")          (TARIFF_INDICATOR)               using (s => s)
  private val currentCons       = extract("""1-0:1\.7\.0\((\d*\.?\d*)\*kW\)""")    (CURRENT_CONSUMPTION)            using (s => BigDecimal(s))
  private val currentProd       = extract("""1-0:2\.7\.0\((\d*\.?\d*)\*kW\)""")    (CURRENT_PRODUCTION)             using (s => BigDecimal(s))

  private val deviceType        = extract("""0-(\d):24\.1\.0\((\w{2,3})\)""")      (EXTERNAL_DEVICE_TYPE)           using ((s1, s2) => (s1.toInt, s2))
  private val deviceEquipmentId = extract("""0-(\d):96\.1\.0\((\w{34})\)""")       (EXTERNAL_DEVICE_EQUIPMENT_ID)   using ((s1, s2) => (s1.toInt, s2))
  private val deviceGasReading  = extract("""0-(\d):24\.2\.1\((\d{12})[SW]\)""" +
                                          """\((\d*\.?\d*)\*m3\)""")               (EXTERNAL_DEVICE_GAS_READING)    using ((s1, s2, s3) => (s1.toInt, LocalDateTime.parse(s2, dateFormat), BigDecimal(s3)))

  private val ignored           = """\d\-\d:\d+[\.:]\d+\.\d+\(.*\)""".r ^^ { case _ => None }

  private val record = versionInfo | dateTimeStamp | equipmentId | elecConsTariff1 | elecConsTariff2 |
    elecProdTariff1 | elecProdTariff2 | tariffIndicator | currentCons | currentProd | deviceType | deviceEquipmentId |
    deviceGasReading | ignored

  private def collectRecords(list: immutable.List[Option[P1Record[_]]]): mutable.Map[P1RecordType, mutable.Set[Any]] = {
    val mm = new mutable.HashMap[P1RecordType, mutable.Set[Any]] with mutable.MultiMap[P1RecordType, Any]

    list.filter(p => p.isDefined)
        .map(p => p.get)
        .foldLeft(mm)((mm, record) => mm.addBinding(record.recordType, record.value))
  }

  private val records = rep(record)

  private val parser: Parser[P1Telegram] = header ~ (records ^^ { x => collectRecords(x) }) ~ checksum ^^ {
    case parsedHeader ~ parsedRecords ~ parsedChecksum =>
      val metadata = P1MetaData(parsedRecords(VERSION_INFORMATION).head.asInstanceOf[String],
                                parsedRecords(DATE_TIME_STAMP).head.asInstanceOf[LocalDateTime],
                                parsedRecords(EQUIPMENT_IDENTIFIER).head.asInstanceOf[String])

      val currentTariff = parsedRecords(TARIFF_INDICATOR).head.asInstanceOf[String]
      val currentConsumption = parsedRecords(CURRENT_CONSUMPTION).head.asInstanceOf[BigDecimal]
      val currentProduction = parsedRecords(CURRENT_PRODUCTION).head.asInstanceOf[BigDecimal]

      val totalConsumption = immutable.Map(("1", parsedRecords(ELECTRICITY_CONSUMED_TARIFF_1).head.asInstanceOf[BigDecimal]),
                                           ("2", parsedRecords(ELECTRICITY_CONSUMED_TARIFF_2).head.asInstanceOf[BigDecimal]))
      val totalProduction = immutable.Map(("1", parsedRecords(ELECTRICITY_PRODUCED_TARIFF_1).head.asInstanceOf[BigDecimal]),
                                          ("2", parsedRecords(ELECTRICITY_PRODUCED_TARIFF_2).head.asInstanceOf[BigDecimal]))

      val data = P1Data(currentTariff, currentConsumption, currentProduction, totalConsumption, totalProduction, None)

      new P1Telegram(parsedHeader, metadata, data, parsedChecksum)
    }

  def parse(text: String): Option[P1Telegram] = parseAll(parser, text) match {
    case Success(result, _) => Some(result)
    case Failure(msg, _)    => handleParseFailure(msg); None
    case Error(msg, _)      => handleParseFailure(msg); None
  }

  private def handleParseFailure(msg: String) = {
    logger.error("Could not parse telegram: {}", msg)
  }
}

case class P1Telegram(header: P1Header,
                      metadata: P1MetaData,
                      data: P1Data,
                      checksum: P1Checksum)

case class P1Header(make: String,
                    identification: String)

case class P1MetaData(versionInfo: String,
                      timestamp: LocalDateTime,
                      equipmentIdentifier: String)

case class P1Data(currentTariff: String,
                  currentConsumption: BigDecimal,
                  currentProduction: BigDecimal,
                  totalConsumption: immutable.Map[String, BigDecimal],
                  totalProduction: immutable.Map[String, BigDecimal],
                  devices: Option[immutable.Seq[P1ExtraDevice]])

case class P1ExtraDevice()

case class P1Checksum(checksum: String)

case class P1GasMeterReading(timestamp: Date, gasDelivered: Double)