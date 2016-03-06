package hyperion

import akka.actor.{Actor, ActorLogging, Props}

object ReceiverActor {
  def props(): Props = {
    Props(new ReceiverActor)
  }
}

class ReceiverActor extends Actor with ActorLogging {
  override def receive = {
    case msg => log.debug("Got message [{}]", msg)
  }
}
