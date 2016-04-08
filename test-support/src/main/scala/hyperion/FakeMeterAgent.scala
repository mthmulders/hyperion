package hyperion

import akka.actor.{Address, ActorSystem, RootActorPath}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.StdIn

object FakeMeterAgent extends App {
  val system = ActorSystem("hyperion-system")
  val path = RootActorPath(Address("akka.tcp", system.name, "localhost", 2552)) / "user" / "receiver"
  val selection = system.actorSelection(path)

  println("Enter to trigger a new telegram, 'q' followed by enter quits")
  while (!"q".equals(StdIn.readLine())) {
    selection ! TelegramReceived(TestSupport.randomTelegram())
  }
  Await.result(system.terminate(), Duration.Inf)
}
