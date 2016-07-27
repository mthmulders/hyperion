package hyperion.rest

import scala.concurrent.ExecutionContext.Implicits.global

import hyperion.{Core, HyperionActors}
import spray.routing.HttpService

/**
  * The REST API layer. It exposes the REST services, but does not provide any web server interface.
  *
  * Notice that it requires to be mixed in with ``hyperion.HyperionActors``, which provides access
  * to the top-level actors that make up the system.
  */
trait RestApi extends HttpService with HyperionActors with Core {
  val restRoutes =
    new RecentReadingsService(recentHistoryActor).route
}