package hyperion

import akka.actor.ActorDSL.actor
import akka.testkit.{TestFSMRef, TestProbe}
import hyperion.MessageDistributor.RegisterReceiver
import hyperion.RecentHistoryActor.{GetRecentHistory, History, RecentReadings, Sleeping}

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
      Thread.sleep(1000)
      messageDispatcher.send(fsm, TelegramReceived(telegram))

      // Assert
      fsm.stateName shouldBe Sleeping
      fsm.stateData shouldBe an[History]
      fsm.stateData.asInstanceOf[History].telegrams.length shouldBe 1
    }

    "return all recent readings when asked" in {
      // Arrange
      val client = TestProbe()
      val messageDispatcher = TestProbe("message-distributor")
      val telegram = TestSupport.randomTelegram()

      // Act
      val sut = actor("retrieve-history")(new RecentHistoryActor(messageDispatcher.ref))
      messageDispatcher.send(sut, TelegramReceived(telegram))
      client.send(sut, GetRecentHistory)

      // Assert
      val result = client.expectMsgClass(classOf[RecentReadings])
      result.telegrams should contain only telegram
    }
  }
}
