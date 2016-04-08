package hyperion

import akka.testkit.{TestProbe, TestFSMRef}
import hyperion.MessageDistributor.RegisterReceiver
import hyperion.RecentHistoryActor.{History, Sleeping}

class RecentHistoryActorSpec extends BaseAkkaSpec {
  "The Recent History Actor" should {
    "register itself with the Message Distributor" in {
      // Arrange
      val messageDistributor = TestProbe("message-distributor")

      // Act
      system.actorOf(RecentHistoryActor.props(messageDistributor.ref), "register")

      // Assert
      messageDistributor.expectMsg(RegisterReceiver)
    }

    "go to sleep after having received one Telegram" in {
      // Arrange
      val messageDispatcher = TestProbe("message-distributor")
      val telegram = TestSupport.randomTelegram()

      // Act
      val fsm = TestFSMRef(new RecentHistoryActor(messageDispatcher.ref), "go-to-sleep")
      messageDispatcher.send(fsm, TelegramReceived(telegram))

      // Assert
      fsm.stateName shouldBe Sleeping
    }

    "wake up after resolution time" in {
      // Arrange
      // Sleep time is set in src/test/resources/application.conf: 100 millis
      val messageDispatcher = TestProbe("message-distributor")
      val telegram = TestSupport.randomTelegram()
      val history = RingBuffer[P1Telegram](2)

      // Act
      val fsm = TestFSMRef(new RecentHistoryActor(messageDispatcher.ref), "wake-up")
      fsm.setState(Sleeping, History(history))
      Thread.sleep(200)
      messageDispatcher.send(fsm, TelegramReceived(telegram))

      // Assert
      fsm.stateName shouldBe Sleeping
      fsm.stateData shouldBe an[History]
      fsm.stateData.asInstanceOf[History].telegrams.length shouldBe 1
    }
  }
}
