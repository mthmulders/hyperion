package hyperion.rest

import java.time.{LocalDate, OffsetDateTime}
import java.time.format.DateTimeFormatter.{ISO_DATE, ISO_OFFSET_DATE_TIME}
import java.time.temporal.Temporal

import hyperion.database.MeterReadingDAO.HistoricalMeterReading
import spray.httpx.SprayJsonSupport
import spray.json._

import scala.util.{Failure, Success, Try}

/** Allows easy mix-in of [[HyperionJsonProtocol]] */
trait HyperionJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {
  implicit def meterReadingFormat: RootJsonFormat[MeterReading] = HyperionJsonProtocol.meterReadingFormat
  implicit def historicalMeterReadingFormat: RootJsonFormat[HistoricalMeterReading] = HyperionJsonProtocol.historicalMeterReadingFormat
}

/** Converts the model of Hyperions REST API into JSON and back */
object HyperionJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {
  implicit object LocalDateFormat extends JsonFormat[LocalDate] {
    override def read(json: JsValue): LocalDate = json match {
      case JsString(value) => Try(LocalDate.parse(value, ISO_DATE)) match {
        case Success(result) => result
        case Failure(cause)  => deserializationError(s"Cannot convert $value to a LocalDate", cause)
      }
      case thing: JsValue    => deserializationError(s"Cannot convert $thing to a LocalDate")
    }

    override def write(input: LocalDate): JsValue = JsString(
      input.format(ISO_DATE)
    )
  }

  implicit object OffsetDateTimeFormat extends JsonFormat[OffsetDateTime] {
    override def read(json: JsValue): OffsetDateTime = json match {
      case JsString(value) => Try(OffsetDateTime.parse(value, ISO_OFFSET_DATE_TIME)) match {
        case Success(result) => result
        case Failure(cause)  => deserializationError(s"Cannot convert $value to a OffsetDateTime", cause)
      }
      case thing: JsValue    => deserializationError(s"Cannot convert $thing to a OffsetDateTime")
    }

    override def write(input: OffsetDateTime): JsValue = JsString(
      input.format(ISO_OFFSET_DATE_TIME)
    )
  }

  implicit val meterReadingFormat = jsonFormat9(MeterReading)
  implicit val historicalMeterReadingFormat = jsonFormat4(HistoricalMeterReading)
}