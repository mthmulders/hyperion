package hyperion

import akka.actor.{ActorRef, ActorLogging, Actor, Props}
import hyperion.rest.{ActualValuesHandlerActor, RecentReadingsHandlerActor}
import spray.can.Http
import spray.http.HttpMethods.GET
import spray.http.{HttpRequest, HttpResponse, StatusCodes, Uri}

/** Companion object voor the [[IncomingHttpActor]] class */
object IncomingHttpActor {
  def props(messageDistributor: ActorRef): Props = {
    Props(new IncomingHttpActor(messageDistributor))
  }
}

/**
  * Acts as a router for incoming HTTP calls.
  */
class IncomingHttpActor(val messageDistributor: ActorRef) extends Actor with ActorLogging {
  val recentReadingsRequestHandler = createRecentReadingsHandlerActor(messageDistributor)

  override def receive: Receive = {
    // When a new TCP connection comes in, we register ourselves as the Actor who will handle it.
    case _: Http.Connected =>
      sender ! Http.Register(self)

    case req @ HttpRequest(GET, Uri.Path("/actual"), _, _, _) =>
      createActualValuesHandlerActor(sender(), messageDistributor) forward req

    case req @ HttpRequest(GET, Uri.Path("/recent"), _, _, _) =>
      recentReadingsRequestHandler forward req

    // All other requests are logged but ignored
    case HttpRequest(method, Uri.Path(path), _, _, _) =>
      log.info("{} request to {} not supported", method, path)
      sender() ! HttpResponse(status = StatusCodes.NotFound, entity = "Not found")
  }

  protected def createRecentReadingsHandlerActor(messageDistributor: ActorRef) = {
    context.actorOf(RecentReadingsHandlerActor.props(messageDistributor))
  }

  protected def createActualValuesHandlerActor(client: ActorRef, messageDistributor: ActorRef) = {
    context.actorOf(ActualValuesHandlerActor.props(client, messageDistributor))
  }
}
