package hyperion.rest

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME

import spray.httpx.SprayJsonSupport
import spray.json._

import scala.util.{Failure, Success, Try}

/** Allows easy mix-in of [[HyperionJsonProtocol]] */
trait HyperionJsonProtocol {
  implicit def meterReadingFormat: RootJsonFormat[MeterReading] = HyperionJsonProtocol.meterReadingFormat
}

/** Converts the model of Hyperions REST API into JSON and back */
object HyperionJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {
  implicit object OffsetDateTimeFormat extends JsonFormat[OffsetDateTime] {
    override def read(json: JsValue): OffsetDateTime = json match {
      case JsString(value) => Try(OffsetDateTime.parse(value, ISO_OFFSET_DATE_TIME)) match {
        case Success(result) => result
        case Failure(cause) => deserializationError(s"Cannot convert $value to a ZonedDateTime", cause)
      }
      case thing: JsValue => deserializationError(s"Cannot convert $thing to a ZonedDateTime")
    }

    override def write(input: OffsetDateTime): JsValue = JsString(
      input.format(ISO_OFFSET_DATE_TIME)
    )
  }

  implicit val meterReadingFormat = jsonFormat9(MeterReading)
}