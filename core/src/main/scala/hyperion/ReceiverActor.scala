package hyperion

import scala.collection.mutable

import akka.actor._
import hyperion.ReceiverActor.RegisterReceiver

object ReceiverActor {
  def props(): Props = {
    Props(new ReceiverActor)
  }

  case object RegisterReceiver
}

class ReceiverActor extends Actor with ActorLogging {
  val recipients = mutable.Buffer.empty[ActorRef]

  override def receive = {
    case RegisterReceiver =>
      log.debug("Adding {} to list of recipients", sender())
      recipients += sender()
      context watch sender()
    case Terminated(ref) =>
      log.debug("Removing {} from list of recipients", ref)
      recipients -= ref
    case msg =>
      log.debug("Forwarding message {}", msg)
      recipients.foreach(_ forward msg)
  }
}
