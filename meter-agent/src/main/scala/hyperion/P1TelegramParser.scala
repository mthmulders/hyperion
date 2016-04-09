package hyperion

import java.time.format.DateTimeFormatter
import java.time.LocalDateTime

import org.slf4j.LoggerFactory

import scala.collection.{mutable, immutable}
import scala.reflect.ClassTag
import util.parsing.combinator.RegexParsers

import P1Constants._
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

    list.flatten.foldLeft(mm)((mm, record) => mm.addBinding(record.recordType, record.value))
  }

  private val records = rep(record)

  private val parser: Parser[P1Telegram] = header ~ (records ^^ { _.flatten}) ~ checksum ^^ {
    case parsedHeader ~ parsedRecords ~ parsedChecksum =>
      def findRecord(recordType: P1RecordType): Option[_] = {
        parsedRecords.find(_.recordType == recordType).map(_.value)
      }
      def findRecords(recordType: P1RecordType): immutable.Seq[Product] = {
        parsedRecords.filter(_.recordType == recordType).map(_.value).map(_.asInstanceOf[Product])
      }
      def findExtraDeviceRecord(recordType: P1RecordType, deviceId: Int): Option[_] = {
        findRecord(recordType).find(_.asInstanceOf[Product].productElement(0) == deviceId)
      }

      val deviceIds: immutable.Seq[Int] = findRecords(EXTERNAL_DEVICE_TYPE)
        .map({ case (deviceId: Int, _: String) => deviceId })
      val devices: immutable.Seq[P1ExtraDevice] = deviceIds.map(deviceId => {
        val deviceTypeRecord = findExtraDeviceRecord(EXTERNAL_DEVICE_TYPE, deviceId)

        deviceTypeRecord.map({ case (_: Int, deviceType: String) => deviceType }) match {
          case Some("003") =>
            val lastCaptureRecord = findExtraDeviceRecord(EXTERNAL_DEVICE_GAS_READING, deviceId)
                .map(_.asInstanceOf[(Int, LocalDateTime, BigDecimal)])

            lastCaptureRecord match {
              case Some((_, captureTime, reading)) =>
                P1GasMeter(deviceId, "003", captureTime, reading)
              case None =>
                logger.warn("External device of type 003 (Gas Meter), but no 0-n:24.2.1.255 line found")
                P1UnknownDevice(deviceId, "003")
            }

          case Some(deviceType) =>
            logger.warn("External device of unsupported type {} found", deviceType)
            P1UnknownDevice(deviceId, deviceType)

          case None =>
            logger.warn("External device found but it did not tell its type")
            P1UnknownDevice(deviceId, "unknown")
        }
      })

      (for {
        versionInfo <- findRecord(VERSION_INFORMATION).map(_.asInstanceOf[String])
        dateTimeStamp <- findRecord(DATE_TIME_STAMP).map(_.asInstanceOf[LocalDateTime])
        equipmentIdentifier <- findRecord(EQUIPMENT_IDENTIFIER).map(_.asInstanceOf[String])
        metadata = P1MetaData(versionInfo, dateTimeStamp, equipmentIdentifier)

        currentTariff <- findRecord(TARIFF_INDICATOR).map(_.asInstanceOf[String])
        currentConsumption <- findRecord(CURRENT_CONSUMPTION).map(_.asInstanceOf[BigDecimal])
        currentProduction <- findRecord(CURRENT_PRODUCTION).map(_.asInstanceOf[BigDecimal])

        consumptionMeter1 <- findRecord(ELECTRICITY_CONSUMED_TARIFF_1).map(_.asInstanceOf[BigDecimal])
        consumptionMeter2 <- findRecord(ELECTRICITY_CONSUMED_TARIFF_2).map(_.asInstanceOf[BigDecimal])
        totalConsumption = immutable.Map(lowTariff -> consumptionMeter1, normalTariff -> consumptionMeter2)

        productionMeter1 <- findRecord(ELECTRICITY_PRODUCED_TARIFF_1).map(_.asInstanceOf[BigDecimal])
        productionMeter2 <- findRecord(ELECTRICITY_PRODUCED_TARIFF_2).map(_.asInstanceOf[BigDecimal])
        totalProduction = immutable.Map(lowTariff -> productionMeter1, normalTariff -> productionMeter2)

        data = P1Data(currentTariff, currentConsumption, currentProduction, totalConsumption, totalProduction, devices)

      } yield P1Telegram(parsedHeader, metadata, data, parsedChecksum)).get
    } | failure("Not all required data objects are found")

  def parse(text: String): Option[P1Telegram] = parseAll(parser, text) match {
    case Success(result, _) => Some(result)
    case Failure(msg, _)    => handleParseFailure(msg); None
    case Error(msg, _)      => handleParseFailure(msg); None
  }

  private def handleParseFailure(msg: String) = {
    logger.error("Could not parse telegram: {}", msg)
  }
}
