package hyperion

import akka.actor.{Actor, ActorLogging}
import spray.http.{HttpEntity, StatusCode, StatusCodes}
import spray.routing.{RejectionHandler, _}
import spray.util.LoggingContext

import scala.util.control.NonFatal

/**
  * Holds potential error response with the HTTP status and optional body
  *
  * @param responseStatus the status code
  * @param response the optional body
  */
case class ErrorResponseException(responseStatus: StatusCode, response: Option[HttpEntity]) extends Exception

/**
  * Allows you to construct Spray ``HttpService`` from a concatenation of routes; and wires in the error handler.
  * It also logs all internal server errors using ``SprayActorLogging``.
  *
  * @param route the (concatenated) route
  */
class RoutedHttpService(route: Route) extends Actor with HttpService with ActorLogging {
  implicit def actorRefFactory = context

  implicit val exceptionHandler = ExceptionHandler {
    case NonFatal(ErrorResponseException(statusCode, entity)) => ctx =>
      ctx.complete((statusCode, entity))

    case NonFatal(e) => ctx => {
      log.error(e, StatusCodes.InternalServerError.defaultMessage)
      ctx.complete(StatusCodes.InternalServerError)
    }
  }

  implicit val rejectionHandler = RejectionHandler.Default
  implicit val routeSettings = RoutingSettings.default
  implicit val loggingContext = LoggingContext.fromActorRefFactory

  def receive: Receive = runRoute(route)
}
