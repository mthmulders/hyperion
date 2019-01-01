package hyperion

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

class DummyActor extends Actor with ActorLogging {
  override def receive: Actor.Receive = {
    case _ => log.warning("No messages expected")
  }
}

trait HyperionIntegrationTestActors extends HyperionActors { this: Core =>
  override val meterAgent: ActorRef = system.actorOf(Props(new DummyActor), "fake-meter-agent")
}