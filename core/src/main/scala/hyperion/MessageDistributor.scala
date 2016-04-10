package hyperion

import scala.collection.mutable

import akka.actor._
import hyperion.MessageDistributor.RegisterReceiver

object MessageDistributor {
  def props(): Props = {
    Props(new MessageDistributor)
  }

  case object RegisterReceiver
}

class MessageDistributor extends Actor with ActorLogging {
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
      log.debug("Forwarding message {} to {} recipients", msg, recipients.length)
      recipients.foreach(_ forward msg)
  }
}
