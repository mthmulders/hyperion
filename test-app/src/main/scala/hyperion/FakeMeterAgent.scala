package hyperion

import akka.actor.{Actor, ActorLogging, ActorRef}

import scala.io.StdIn

class FakeMeterAgent(messageDistributor: ActorRef, settings: AppSettings) extends Actor with ActorLogging {
  override def receive = {
    case _ =>
      log.warning("No messages expected")
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