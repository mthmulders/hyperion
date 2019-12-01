package hyperion.ws

import akka.actor.{Actor, ActorLogging, ActorRef}

import hyperion.MessageDistributor.RegisterReceiver
import hyperion.p1.TelegramReceived

/**
  * Actor that forwards telegrams to a WebSocket connection
  *
  * @param source Ref to an Actor that publishes the telegrams to a [[Stream]].
  * @param messageDistributor Ref to the Actor that distributes messages.
  */
class ActualValuesHandlerActor(val source: ActorRef, val messageDistributor: ActorRef) extends Actor with ActorLogging {
  override def preStart(): Unit = {
    log.debug("Registering for live updates")
    messageDistributor ! RegisterReceiver
  }

  override def receive: Receive = {
    case tr: TelegramReceived =>
      source ! tr
    case a: Any =>
      log.debug(s"Ignoring $a")
  }
}
