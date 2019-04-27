package hyperion

import akka.actor.{Actor, ActorLogging, DeadLetter, Props}

object DeadLetterLoggingActor {
  def props(): Props = Props(new DeadLetterLoggingActor())
}

class DeadLetterLoggingActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case DeadLetter(msg, sender, recipient) =>
      val message = s"Could not deliver [$msg] from ${sender.path} to ${recipient.path}"
      log.info(message)
  }
}
