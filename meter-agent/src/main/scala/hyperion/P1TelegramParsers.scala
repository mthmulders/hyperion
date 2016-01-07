package hyperion

import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
import java.util.Date

import scala.collection.{mutable, immutable}
import scala.reflect.ClassTag
import util.parsing.combinator.RegexParsers

trait P1TelegramParsers extends RegexParsers {
  import P1RecordType._

  case class P1Record[T](recordType: P1RecordType, value: T)
  object parsers {
    private val headerPattern = """\/([A-Za-z0-9]{3})5(.*)""".r
    val header = headerPattern ^^ { case headerPattern(make, identification) => P1Header(make, identification)}

    private val checksumPattern = """!([A-Za-z0-9]{4})""".r
    val checksum = checksumPattern ^^ { case checksumPattern(value) => P1Checksum(value) }

    private val dateFormat = DateTimeFormatter.ofPattern("yyMMddHHmmss")
    private class ParseAs(val s: String) {
      private var recordType: P1RecordType = null
      def to(recordType: P1RecordType) = {
        this.recordType = recordType
        this
      }

      def using[T: ClassTag](): Parser[Option[P1Record[T]]] = {
        val compiledPattern = s.r
        compiledPattern ^^ {
          case compiledPattern        => None
        }
      }
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
    private def extract[T:ClassTag](s: String) = new ParseAs(s)

    // Patterns and parsers for various P1 Records
    val versionInfo       = extract("""1-3:0\.2\.8\((\d*)\)""")              to VERSION_INFORMATION           using (s => s)
    val dateTimeStamp     = extract("""0-0:1\.0\.0\((\d{12})[SW]\)""")       to DATE_TIME_STAMP               using (s => LocalDateTime.parse(s, dateFormat))
    val equipmentId       = extract("""0-0:96\.1\.1\((\w*)\)""")             to EQUIPMENT_IDENTIFIER          using (s => s)
    val elecConsTariff1   = extract("""1-0:1\.8\.1\((\d*\.?\d*)\*kWh\)""")   to ELECTRICITY_CONSUMED_TARIFF_1 using (s => BigDecimal(s))
    val elecConsTariff2   = extract("""1-0:1\.8\.2\((\d*\.?\d*)\*kWh\)""")   to ELECTRICITY_CONSUMED_TARIFF_2 using (s => BigDecimal(s))
    val elecProdTariff1   = extract("""1-0:2\.8\.1\((\d*\.?\d*)\*kWh\)""")   to ELECTRICITY_PRODUCED_TARIFF_1 using (s => BigDecimal(s))
    val elecProdTariff2   = extract("""1-0:2\.8\.2\((\d*\.?\d*)\*kWh\)""")   to ELECTRICITY_PRODUCED_TARIFF_2 using (s => BigDecimal(s))
    val tariffIndicator   = extract("""0-0:96\.14\.0\((\d{4})\)""")          to TARIFF_INDICATOR              using (s => s)
    val currentCons       = extract("""1-0:1\.7\.0\((\d*\.?\d*)\*kW\)""")    to CURRENT_CONSUMPTION           using (s => BigDecimal(s))
    val currentProd       = extract("""1-0:2\.7\.0\((\d*\.?\d*)\*kW\)""")    to CURRENT_PRODUCTION            using (s => BigDecimal(s))

    val deviceType        = extract("""0-(\d):24\.1\.0\((\w{2,3})\)""")      to EXTERNAL_DEVICE_TYPE          using ((s1, s2) => (s1.toInt, s2))
    val deviceEquipmentId = extract("""0-(\d):96\.1\.0\((\w{34})\)""")       to EXTERNAL_DEVICE_EQUIPMENT_ID  using ((s1, s2) => (s1.toInt, s2))
    val deviceGasReading  = extract("""0-(\d):24\.2\.1\((\d{12})[SW]\)""" +
                                    """\((\d*\.?\d*)\*m3\)""")               to EXTERNAL_DEVICE_GAS_READING     using ((s1, s2, s3) => (s1.toInt, LocalDateTime.parse(s2, dateFormat), BigDecimal(s3)))

    val ignored           = extract("""\d\-\d:\d+[\.:]\d+\.\d+\(.*\)""")                                      using ()

    val record = versionInfo | dateTimeStamp | equipmentId | elecConsTariff1 | elecConsTariff2 |
      elecProdTariff1 | elecProdTariff2 | tariffIndicator | currentCons | currentProd | deviceType | deviceEquipmentId |
      deviceGasReading | ignored
  }

  private def collectRecords(list: immutable.List[Option[P1Record[_]]]): mutable.Map[P1RecordType, mutable.Set[Any]] = {
    val mm = new mutable.HashMap[P1RecordType, mutable.Set[Any]] with mutable.MultiMap[P1RecordType, Any]

    list.filter(p => p.isDefined)
        .map(p => p.get)
        .foldLeft(mm)((mm, record) => mm.addBinding(record.recordType, record.value))
  }

  def p1parser: Parser[P1Telegram] =
    parsers.header ~
    (rep(parsers.record) ^^ { x => collectRecords(x) }) ~
    parsers.checksum ^^ { case header ~ records ~ checksum =>
      val metadata = P1MetaData(records(VERSION_INFORMATION).head.asInstanceOf[String],
                                records(DATE_TIME_STAMP).head.asInstanceOf[LocalDateTime],
                                records(EQUIPMENT_IDENTIFIER).head.asInstanceOf[String])

      val currentTariff = records(TARIFF_INDICATOR).head.asInstanceOf[String]
      val currentConsumption = records(CURRENT_CONSUMPTION).head.asInstanceOf[BigDecimal]
      val currentProduction = records(CURRENT_PRODUCTION).head.asInstanceOf[BigDecimal]

      val totalConsumption = immutable.Map(("1", records(ELECTRICITY_CONSUMED_TARIFF_1).head.asInstanceOf[BigDecimal]),
                                           ("2", records(ELECTRICITY_CONSUMED_TARIFF_2).head.asInstanceOf[BigDecimal]))
      val totalProduction = immutable.Map(("1", records(ELECTRICITY_PRODUCED_TARIFF_1).head.asInstanceOf[BigDecimal]),
                                          ("2", records(ELECTRICITY_PRODUCED_TARIFF_2).head.asInstanceOf[BigDecimal]))

      val data = P1Data(currentTariff, currentConsumption, currentProduction, totalConsumption, totalProduction, None)

      new P1Telegram(header, metadata, data, checksum)
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