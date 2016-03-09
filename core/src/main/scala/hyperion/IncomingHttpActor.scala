package hyperion

import akka.actor.{ActorLogging, Actor, Props}

object IncomingHttpActor {
  def props(): Props = {
    Props(new IncomingHttpActor())
  }
}

class IncomingHttpActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case msg: Any => log.debug("Message {} not handled", msg)
  }
}
