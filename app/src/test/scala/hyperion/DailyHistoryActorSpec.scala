package hyperion

import akka.testkit.{TestFSMRef, TestProbe}
import hyperion.MessageDistributor.RegisterReceiver
import hyperion.DailyHistoryActor.{Empty, Receiving, Sleeping}

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

    "wake up after resolution time" in {
      // Arrange
      // Sleep time is set in src/test/resources/application.conf: 100 millis
      val messageDispatcher = TestProbe("message-distributor")
      val telegram = TestSupport.randomTelegram()

      // Act
      val fsm = TestFSMRef(new DailyHistoryActor(messageDispatcher.ref), "daily-wake-up")
      fsm.setState(Sleeping, Empty)
      Thread.sleep(1000)

      // Assert
      fsm.stateName shouldBe Receiving
    }

    "store telegrams in database" in {
      val messageDispatcher = TestProbe("message-distributor")
      val telegram = TestSupport.randomTelegram()
      val history = RingBuffer[P1Telegram](2)

      // Act
      val fsm = TestFSMRef(new RecentHistoryActor(messageDispatcher.ref), "recent-store")
      messageDispatcher.send(fsm, TelegramReceived(telegram))

      // Assert

    }
  }
}
