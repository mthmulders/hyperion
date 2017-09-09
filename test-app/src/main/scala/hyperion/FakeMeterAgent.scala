package hyperion

import scala.io.StdIn

import akka.actor.{Actor, ActorLogging, ActorRef}

class FakeMeterAgent(messageDistributor: ActorRef) extends Actor with ActorLogging {
  override def receive: Actor.Receive = {
    case _ => log.warning("No messages expected")
  }

  log.warning("+--------------------------------------------------------------+")
  log.warning("| Enter to trigger a new telegram, 'q' followed by enter quits |")
  log.warning("+--------------------------------------------------------------+")
  while (!"q".equals(StdIn.readLine())) {
    val telegram = TestSupport.randomTelegram()
    log.info("Sending fake telegram {}", telegram)
    messageDistributor ! TelegramReceived(telegram)
  }
}