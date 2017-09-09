package hyperion.rest

import java.time.LocalDate

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}
import akka.pattern.ask
import akka.util.Timeout

import hyperion.DailyHistoryActor.{RetrieveMeterReading, RetrievedMeterReading}

/**
  * Provides the Spray route to retrieve a meter reading by date from the database.
  * @param dailyHistoryActor Ref to the ``DailyHistoryActor``.
  */
class DailyHistoryService(dailyHistoryActor: ActorRef)(implicit executionContext: ExecutionContext)
  extends Directives with HyperionJsonProtocol {

  implicit val timeout: Timeout = Timeout(2 seconds)

  val route: Route = path("history") {
    get {
      parameters('date.as[LocalDate]) { date =>
        val query = (dailyHistoryActor ? RetrieveMeterReading(date)).mapTo[RetrievedMeterReading]
        onSuccess(query) { result =>
          complete(result.reading match {
            case None => (StatusCodes.NotFound, s"No record found for date $date")
            case Some(reading) => (StatusCodes.OK, reading)
          })
        }
      }
    }
  }

}
