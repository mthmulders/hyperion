package hyperion

import akka.actor.{ActorRef, ActorLogging, Actor, Props}
import spray.can.Http
import spray.http.{HttpRequest, HttpResponse, StatusCodes, Uri}

/** Companion object voor the [[IncomingHttpActor]] class */
object IncomingHttpActor {
  def props(messageDistributor: ActorRef): Props = {
    Props(new IncomingHttpActor(messageDistributor))
  }
}

/**
  * Acts as a router for incoming HTTP calls.
  * @param messageDistributor: ref to the actor that distributes messages
  */
class IncomingHttpActor(messageDistributor: ActorRef) extends Actor with ActorLogging {
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
