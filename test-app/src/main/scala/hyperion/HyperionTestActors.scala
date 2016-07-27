package hyperion

import akka.actor.Props

trait HyperionTestActors extends HyperionActors { this: Core =>
  override val meterAgent = system.actorOf(Props(new MeterAgent(messageDistributor, settings)), "meter-agent")
}
