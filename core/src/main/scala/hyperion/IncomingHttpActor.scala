package hyperion

import akka.actor.{ActorLogging, Actor, Props}
import spray.can.Http
import spray.http.{HttpRequest, HttpResponse, StatusCodes, Uri}

/** Companion object voor the [[IncomingHttpActor]] class */
object IncomingHttpActor {
  def props(): Props = {
    Props(new IncomingHttpActor())
  }
}

/**
  * Acts as a router for incoming HTTP calls.
  */
class IncomingHttpActor extends Actor with ActorLogging {
  override def receive: Receive = {
    // When a new TCP connection comes in, we register ourselves as the Actor who will handle it.
    case _: Http.Connected =>
      sender ! Http.Register(self)

    // All other requests are logged but ignored
    case HttpRequest(method, Uri.Path(path), _, _, _) =>
      log.info("{} request to {} not supported", method, path)
      sender() ! HttpResponse(status = StatusCodes.NotFound, entity = "Not found")
  }
}
