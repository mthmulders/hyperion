package hyperion.rest

import java.time.{LocalDate, Month}

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}
import akka.pattern.ask
import akka.util.Timeout

import hyperion.database.DatabaseActor._

/**
  * Provides the Spray route to retrieve a meter reading by date from the database.
  * @param databaseActor Ref to the ``DatabaseActor``.
  */
class HistoryService(databaseActor: ActorRef)(implicit executionContext: ExecutionContext)
  extends Directives with HyperionJsonProtocol {

  implicit val timeout: Timeout = Timeout(5 seconds)

  val route: Route = path("history") {
    get {
      parameters('date.as[LocalDate]) { date =>
        val query = (databaseActor ? RetrieveMeterReadingForDate(date)).mapTo[RetrievedMeterReadings]
        onSuccess(query) { result =>
          complete(result.readings match {
            case Seq()            => (StatusCodes.NotFound, None)
            case Seq(item)        => (StatusCodes.OK, item)
          })
        }
      } ~
      parameters('month.as[Month], 'year.as[Int]) { (month, year) =>
        val query = (databaseActor ? RetrieveMeterReadingForMonth(month, year)).mapTo[RetrievedMeterReadings]
        onSuccess(query) { result =>
          complete(result.readings match {
            case Seq()            => (StatusCodes.NotFound, None)
            case Seq(items @ _ *) => (StatusCodes.OK, items)
          })
        }
      }
    }
  }
}
