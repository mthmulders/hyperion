package hyperion.rest

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import hyperion.RecentHistoryActor.{GetRecentHistory, RecentReadings}
import hyperion.rest.HyperionConversions.telegramWrapper
import scala.concurrent.duration.DurationInt
import spray.http.ContentTypes.`application/json`
import spray.http.StatusCodes.OK
import spray.http.HttpMethods.GET
import spray.http.HttpProtocols.`HTTP/1.1`
import spray.http.{HttpEntity, HttpRequest, HttpResponse, StatusCodes, Uri}
import spray.json._
import spray.json.DefaultJsonProtocol._

object RecentReadingsHandlerActor {
  def props(recentHistoryActor: ActorRef): Props = {
    Props(new RecentReadingsHandlerActor(recentHistoryActor))
  }
}

class RecentReadingsHandlerActor(recentHistoryActor: ActorRef) extends Actor
  with ActorLogging with HyperionJsonProtocol {

  implicit val timeout = Timeout(500 millis)
  import context.dispatcher

  override def receive: Receive = {
    case HttpRequest(GET, Uri.Path("/recent"), _, _, _) =>
      val client = sender()
      val future = recentHistoryActor ? GetRecentHistory
      future map {
        case RecentReadings(history) =>
          log.debug("Got a RecentReadings message with {} readings", history.length)
          val json = history.map(telegramWrapper).toJson
          val entity = HttpEntity(`application/json`, json.toString())
          log.debug("Sending back response to {}", client)
          client ! HttpResponse(OK, entity, Nil, `HTTP/1.1`)
        case msg: Any =>
          log.warning("Expected a RecentReadings but got [{}]", msg)
          client ! HttpResponse(status = StatusCodes.InternalServerError)
      }

    case HttpRequest(method, path, _, _, _) =>
      log.warning("Should not receive HTTP {}-request to {}", method, path)
      sender() ! HttpResponse(status = StatusCodes.BadRequest)

    case msg: Any =>
      log.warning("Should not receive message [{}]", msg)
      sender() ! HttpResponse(status = StatusCodes.BadRequest)

  }
}
