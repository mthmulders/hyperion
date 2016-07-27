package hyperion.ws

import akka.actor.ActorRef
import hyperion.MessageDistributor.RegisterReceiver
import hyperion.TelegramReceived
import hyperion.rest.HyperionConversions.telegramWrapper
import hyperion.rest.HyperionJsonProtocol
import spray.can.websocket.WebSocketServerWorker
import spray.can.websocket.frame.TextFrame

/**
  * Worker that forwards incoming telegrams in JSON-format over a WebSocket to a connected client.
  * @param messageDistributor Ref to the Message Distributor
  * @param client The client that will receive the telegrams
  */
class ActualReadingsClientWorker(val messageDistributor: ActorRef,
                                 val client: ActorRef)
  extends WebSocketServerWorker
    with HyperionJsonProtocol {

  override def preStart = messageDistributor ! RegisterReceiver

  override def serverConnection: ActorRef = client

  override def receive = handshaking orElse businessLogic orElse closeLogic

  override def businessLogic: Receive = {
    case TelegramReceived(telegram) =>
      val reading = telegram
      val json = meterReadingFormat.write(reading)
      send(TextFrame(json.toString()))
  }

}