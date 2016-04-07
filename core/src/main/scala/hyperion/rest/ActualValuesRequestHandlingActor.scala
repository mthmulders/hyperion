package hyperion.rest

import akka.actor.{ActorLogging, ActorRef, Props}
import hyperion.MessageDistributor.RegisterReceiver
import hyperion.{P1GasMeter, TelegramReceived}
import spray.can.websocket
import spray.can.websocket.frame.TextFrame
import spray.routing.HttpServiceActor
import spray.json._

object ActualValuesRequestHandlingActor {
  def props(httpClient: ActorRef, messageDistributor: ActorRef) = {
    Props(new ActualValuesRequestHandlingActor(httpClient, messageDistributor))
  }
}

/**
  * Actor that upgrades an HTTP connection to a WebSocket connection and then updates the client
  * when new meter readings come in.
  *
  * @param httpClient Ref to the Actor that does the communication with the client.
  * @param messageDistributor Ref to the Actor that distributes messages.
  */
class ActualValuesRequestHandlingActor(val httpClient: ActorRef, val messageDistributor: ActorRef) extends HttpServiceActor
  with ActorLogging with websocket.WebSocketServerWorker with HyperionJsonProtocol {

  override def receive = handshaking orElse businessLogic orElse closeLogic

  override def preStart = messageDistributor ! RegisterReceiver

  def businessLogic: Receive = {
    case TelegramReceived(telegram) =>
      val gasConsumption = telegram.data.devices
        .find(_.isInstanceOf[P1GasMeter])
        .map(_.asInstanceOf[P1GasMeter].gasDelivered)
      val msg = MeterReading(
        telegram.metadata.timestamp,
        telegram.data.currentTariff,
        telegram.data.currentConsumption,
        telegram.data.currentProduction,
        gasConsumption)
      send(TextFrame(msg.toJson.toString))
  }

  override def serverConnection: ActorRef = {
    httpClient
  }
}
