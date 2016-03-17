package hyperion.rest

import java.time.{ZoneOffset, LocalDateTime}
import java.time.format.DateTimeFormatter
import spray.json._
import scala.util.{Try, Failure, Success}

/** Allows easy mix-in of [[HyperionJsonProtocol]] */
trait HyperionJsonProtocol {
  implicit val meterReadingFormat = HyperionJsonProtocol.meterReadingFormat
}

/** Converts the model of Hyperions REST API into JSON and back */
object HyperionJsonProtocol extends DefaultJsonProtocol {
  implicit object LocalDateDimeFormat extends JsonFormat[LocalDateTime] {
    private[this] val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")

    override def read(json: JsValue): LocalDateTime = json match {
      case JsString(value) => Try(LocalDateTime.parse(value, format)) match {
        case Success(result) => result
        case Failure(cause) => throw new DeserializationException(s"Cannot convert $value to a LocalDateTime", cause)
      }
      case thing: JsValue => throw new DeserializationException(s"Cannot convert $thing to a LocalDateTime")
    }

    override def write(input: LocalDateTime): JsValue = JsString(input.atOffset(ZoneOffset.UTC).format(format))
  }

  implicit val meterReadingFormat = jsonFormat5(MeterReading)
}