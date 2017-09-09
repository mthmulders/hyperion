package hyperion

import akka.actor.Props

import org.slf4j.LoggerFactory

trait HyperionTestActors extends HyperionActors { this: Core =>
  LoggerFactory.getLogger(getClass).warn("Overriding meter agent with a fake one")

  override val meterAgent = system.actorOf(Props(new FakeMeterAgent(messageDistributor)), "fake-meter-agent")
}
