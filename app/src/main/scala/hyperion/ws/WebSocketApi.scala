package hyperion.ws

import scala.concurrent.ExecutionContext.Implicits.global

import hyperion.{Core, HyperionActors}
import hyperion.rest.HyperionJsonProtocol

/**
  * The WebSocket API layer. It exposes the WebSocket services, but does not provide any web server interface.
  *
  * Notice that it requires to be mixed in with ``hyperion.HyperionActors``, which provides access
  * to the top-level actors that make up the system.
  */
trait WebSocketApi extends Core with HyperionActors with HyperionJsonProtocol {
  val webSocketRoutes =
    new ActualReadingsService(messageDistributor, system).route
}
