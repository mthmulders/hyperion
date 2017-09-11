package hyperion.rest

import java.time.{LocalDate, OffsetDateTime}
import java.time.format.DateTimeFormatter.{ISO_DATE, ISO_OFFSET_DATE_TIME}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.Materializer
import spray.json._

import hyperion.database.HistoricalMeterReading

/** Allows easy mix-in of [[HyperionJsonProtocol]] */
trait HyperionJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {
  implicit def meterReadingFormat: RootJsonFormat[MeterReading] = HyperionJsonProtocol.meterReadingFormat
  implicit def historicalMeterReadingFormat: RootJsonFormat[HistoricalMeterReading] = HyperionJsonProtocol.historicalMeterReadingFormat
  implicit def localDateUnmarshaller: Unmarshaller[String, LocalDate] = HyperionJsonProtocol.localDateUnmarshaller
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

  implicit val localDateUnmarshaller: Unmarshaller[String, LocalDate] = new Unmarshaller[String, LocalDate] {
    override def apply(value: String)(implicit ec: ExecutionContext, materializer: Materializer): Future[LocalDate] = {
      Try(LocalDate.parse(value, ISO_DATE)) match {
        case Success(date) => Future { date }
        case Failure(reason) => Future.failed(reason)
      }
    }
  }

  val meterReadingFormat: RootJsonFormat[MeterReading] = jsonFormat9(MeterReading)
  val historicalMeterReadingFormat: RootJsonFormat[HistoricalMeterReading] = jsonFormat4(HistoricalMeterReading)
}