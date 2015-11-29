package hyperion

import akka.actor.{Props, Actor, ActorLogging}

object MeterAgent {
  def props(): Props = {
    Props(new MeterAgent)
  }
}

class MeterAgent extends Actor with ActorLogging {
  override def receive: Receive = {
    case a: Any => log.debug(s"Ignoring message $a")
  }
}
