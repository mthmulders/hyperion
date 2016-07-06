package hyperion

import akka.testkit.{TestFSMRef, TestProbe}
import hyperion.MessageDistributor.RegisterReceiver
import hyperion.DailyHistoryActor.Sleeping

class DailyHistoryActorSpec extends BaseAkkaSpec {
  "The Daily History Actor" should {
    "register itself with the Message Distributor" in {
      // Arrange
      val messageDistributor = TestProbe("recemessage-distributor")

      // Act
      system.actorOf(DailyHistoryActor.props(messageDistributor.ref), "daily-register")

      // Assert
      messageDistributor.expectMsg(RegisterReceiver)
    }

    "go to sleep after having received one Telegram" in {
      // Arrange
      val messageDispatcher = TestProbe("message-distributor")
      val telegram = TestSupport.randomTelegram()

      // Act
      val fsm = TestFSMRef(new DailyHistoryActor(messageDispatcher.ref), "daily-go-to-sleep")
      messageDispatcher.send(fsm, TelegramReceived(telegram))

      // Assert
      fsm.stateName shouldBe Sleeping
    }
  }
}
