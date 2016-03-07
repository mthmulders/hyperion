package hyperion

import scala.collection.mutable

import akka.actor.{ActorRef, Actor, ActorLogging, Props}
import hyperion.ReceiverActor.RegisterReceiver

object ReceiverActor {
  def props(): Props = {
    Props(new ReceiverActor)
  }

  case object RegisterReceiver
}

class ReceiverActor extends Actor with ActorLogging {
  val receivers = mutable.Buffer.empty[ActorRef]

  override def receive = {
    case RegisterReceiver =>
      receivers += sender()
    case msg =>
      log.debug("Got message [{}]", msg)
      receivers.foreach(_ forward msg)
  }
}
