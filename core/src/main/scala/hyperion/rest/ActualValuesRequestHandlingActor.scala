package hyperion.rest

import akka.actor._
import hyperion.MessageDistributor.RegisterReceiver
import hyperion.{P1GasMeter, TelegramReceived}
import spray.can.websocket
import spray.can.websocket.frame.TextFrame
import spray.routing.HttpServiceActor
import spray.json._

object ActualValuesRequestHandlingActor {
  def props(httpClient: ActorRef) = {
    Props(new ActualValuesRequestHandlingActor(httpClient))
  }
}

/**
  * Actor that upgrades an HTTP connection to a WebSocket connection and then updates the client
  * when new meter readings come in.
  *
  * @param httpClient Ref to the Actor that does the communication with the client.
  */
class ActualValuesRequestHandlingActor(val httpClient: ActorRef) extends HttpServiceActor
  with ActorLogging with websocket.WebSocketServerWorker with HyperionJsonProtocol {

  override def receive = handshaking orElse businessLogic orElse closeLogic

  override def preStart = {
    context.actorSelection("/user/receiver") ! RegisterReceiver
  }

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
