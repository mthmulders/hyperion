package hyperion.rest

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
import spray.json._
import scala.util.{Try, Failure, Success}

/** Allows easy mix-in of [[HyperionJsonProtocol]] */
trait HyperionJsonProtocol {
  implicit val meterReadingFormat = HyperionJsonProtocol.meterReadingFormat
}

/** Converts the model of Hyperions REST API into JSON and back */
object HyperionJsonProtocol extends DefaultJsonProtocol {
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