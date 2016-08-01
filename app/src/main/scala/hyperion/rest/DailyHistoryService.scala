package hyperion.rest

import java.time.LocalDate
import java.time.format.DateTimeFormatter.ISO_DATE

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import hyperion.DailyHistoryActor.{RetrieveMeterReading, RetrievedMeterReading}
import hyperion.database.MeterReadingDAO.HistoricalMeterReading
import org.slf4j.LoggerFactory
import spray.http.StatusCodes
import spray.httpx.marshalling.ToResponseMarshallable
import spray.httpx.unmarshalling.{Deserializer, MalformedContent}
import spray.routing.Directives

import scala.concurrent.ExecutionContext

/**
  * Provides the Spray route to retrieve a meter reading by date from the database.
  * @param dailyHistoryActor Ref to the ``DailyHistoryActor``.
  */
class DailyHistoryService(dailyHistoryActor: ActorRef)(implicit executionContext: ExecutionContext)
  extends Directives with HyperionJsonProtocol {
  private[this] val logger = LoggerFactory.getLogger(getClass)

  implicit val timeout = Timeout(15 seconds)

  implicit val localDateDeserializer = new Deserializer[String, LocalDate] {
    def apply(value: String) = Try(LocalDate.parse(value, ISO_DATE)) match {
      case Success(date) =>
        Right(date)
      case Failure(reason) =>
        logger.error(s"Could not parse $value as date due to", reason)
        Left(MalformedContent(s"'$value' is not a valid date value"))
    }
  }

  val route = path("history") {
    get {
      parameter('date.as[LocalDate] ) { implicit date =>
        complete {
          (dailyHistoryActor ? RetrieveMeterReading(date))
            .mapTo[RetrievedMeterReading]
            .map(_.reading)
            .map(mapToResponse)
        }
      }
    }
  }

  private def mapToResponse(value: Option[HistoricalMeterReading])(implicit date: LocalDate): ToResponseMarshallable = value match {
    case Some(reading) => (StatusCodes.OK, reading)
    case None          => (StatusCodes.NotFound, s"No record found for date $date")
  }
}
