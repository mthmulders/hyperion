package hyperion.rest

import scala.concurrent.ExecutionContext.Implicits.global

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import hyperion.ws.ActualReadingsService

import hyperion.{Core, HyperionActors}

/**
  * The REST API layer. It exposes the REST services, but does not provide any web server interface.
  *
  * Notice that it requires to be mixed in with ``hyperion.HyperionActors``, which provides access
  * to the top-level actors that make up the system.
  */
trait HttpApi extends HyperionActors with Core {
  val routes: Route =
    new RecentReadingsService(recentHistoryActor).route ~
    new HistoryService(databaseActor).route ~
    new AppInfoService().route ~
    new ActualReadingsService(messageDistributor, system).route ~
    new UsageService(usageCalculationActor).route
}