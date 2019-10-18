package hyperion.rest

import java.time.{LocalDate, Month}

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}
import akka.pattern.ask
import akka.util.Timeout
import hyperion.UsageCalculationActor.{CalculateUsage, UsageDataRecord}

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt


/**
  * Provides the Spray route to retrieve usage data by month from the database.
  *
  * @param usageCalculationActor Ref to the ``UsageCalculationActor``.
  */
class UsageService(usageCalculationActor: ActorRef)(implicit executionContext: ExecutionContext)
  extends Directives with HyperionJsonProtocol {

  implicit val timeout: Timeout = Timeout(5 seconds)

  val route: Route = path("usage") {
    get {
      parameters(Symbol("month").as[Month], Symbol("year").as[Int]) { (month, year) =>
        val start = LocalDate.of(year, month, 1)
        val end = start.plusMonths(1)
        val query = (usageCalculationActor ? CalculateUsage(start, end)).mapTo[Seq[UsageDataRecord]]
        onSuccess(query) { result =>
          complete(result match {
            case Seq()            => (StatusCodes.NotFound, None)
            case Seq(items @ _ *) => (StatusCodes.OK, items)
          })
        }
      }
    }
  }
}
