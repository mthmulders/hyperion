package hyperion

import akka.actor.{ActorSystem, Address, RootActorPath}

import scala.concurrent.Future
import scala.io.StdIn
import scala.concurrent.ExecutionContext.Implicits.global

object HyperionTestApp extends App {
  val system = ActorSystem("hyperion-system")
  new HyperionApp(system)

  val path = RootActorPath(Address("akka", system.name)) / "user" / "receiver"
  val selection = system.actorSelection(path)

  Future { Thread.sleep(2000); true } onComplete {
    case _ =>
      system.log.warning("+--------------------------------------------------------------+")
      system.log.warning("| Enter to trigger a new telegram, 'q' followed by enter quits |")
      system.log.warning("+--------------------------------------------------------------+")
      while (!"q".equals(StdIn.readLine())) {
        val telegram = TestSupport.randomTelegram()
        system.log.info("Sending fake telegram {}", telegram)
        selection ! TelegramReceived(telegram)
      }
  }
}
