package hyperion.rest

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import hyperion.RecentHistoryActor.{GetRecentHistory, RecentReadings}
import hyperion.rest.HyperionConversions.telegramWrapper
import spray.httpx.SprayJsonSupport
import spray.json._
import spray.routing.Directives

class RecentReadingsService(recentHistoryActor: ActorRef)(implicit executionContext: ExecutionContext)
  extends Directives with HyperionJsonProtocol with DefaultJsonProtocol with SprayJsonSupport {

  implicit val timeout = Timeout(500 milliseconds)

  val route = path("recent") {
    get {
      complete {
        val response: Future[Vector[MeterReading]] = (recentHistoryActor ? GetRecentHistory)
          .mapTo[RecentReadings]
          .map(_.telegrams)
          .map(_.map(telegramWrapper))
        response
      }
    }
  }
}
