package hyperion.rest

import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext

import akka.actor.ActorRef
import akka.http.scaladsl.server.{Directives, Route}
import akka.pattern.ask
import akka.util.Timeout

import hyperion.RecentHistoryActor.{GetRecentHistory, RecentReadings}

/**
  * Provides the Spray route to retrieve the most recent meter readings from memory.
  * @param recentHistoryActor Ref to the ``RecentHistoryActor``.
  * @param executionContext An ``ExecutionContext``.
  */
class RecentReadingsService(recentHistoryActor: ActorRef)(implicit executionContext: ExecutionContext)
  extends Directives with HyperionJsonProtocol with HyperionConversions {

  implicit val timeout: Timeout = Timeout(5 seconds)

  val route: Route = path("recent") {
    get {
      complete {
        (recentHistoryActor ? GetRecentHistory)
          .mapTo[RecentReadings]
          .map(_.telegrams)
          .map(_.map(p1Telegram2MeterReading))
      }
    }
  }
}
