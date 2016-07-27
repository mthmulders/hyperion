package hyperion.rest

import akka.actor.{ActorLogging, ActorRef}
import hyperion.TelegramReceived
import hyperion.MessageDistributor.RegisterReceiver
import hyperion.rest.HyperionConversions.telegramWrapper
import spray.can.websocket.WebSocketServerWorker
import spray.can.websocket.frame.TextFrame
import spray.routing.HttpServiceActor
import spray.json._

/**
  * Actor that upgrades an HTTP connection to a WebSocket connection and then updates the client
  * when new meter readings come in.
  *
  * @param httpClient Ref to the Actor that does the communication with the client.
  * @param messageDistributor Ref to the Actor that distributes messages.
  */
class ActualValuesHandlerActor(val httpClient: ActorRef, val messageDistributor: ActorRef) extends HttpServiceActor
  with ActorLogging with WebSocketServerWorker with HyperionJsonProtocol {

  override def receive = handshaking orElse businessLogic orElse closeLogic

  override def preStart = messageDistributor ! RegisterReceiver

  def businessLogic: Receive = {
    case TelegramReceived(telegram) =>
      val reading: MeterReading = telegram
      send(TextFrame(reading.toJson.toString))
  }

  override def serverConnection: ActorRef = {
    httpClient
  }
}
