package hyperion

import java.time.LocalDate

import akka.actor.FSM.StateTimeout
import akka.actor.Props
import akka.testkit.{TestFSMRef, TestProbe}
import hyperion.MessageDistributor.RegisterReceiver
import hyperion.DailyHistoryActor.{Empty, Receiving, Sleeping, StoreMeterReading}
import hyperion.database.MeterReadingDAO.MeterReading

class DailyHistoryActorSpec extends BaseAkkaSpec {
  "The Daily History Actor" should {
    "register itself with the Message Distributor" in {
      // Arrange
      val messageDistributor = TestProbe("recemessage-distributor")

      // Act
      system.actorOf(Props(new DailyHistoryActor(messageDistributor.ref, settings)), "daily-register")

      // Assert
      messageDistributor.expectMsg(RegisterReceiver)
    }

    "go to sleep after having received one Telegram" in {
      // Arrange
      val messageDispatcher = TestProbe("message-distributor")
      val telegram = TestSupport.randomTelegram()

      // Act
      val fsm = TestFSMRef(new DailyHistoryActor(messageDispatcher.ref, settings), "daily-go-to-sleep")
      messageDispatcher.send(fsm, TelegramReceived(telegram))

      // Assert
      fsm.stateName shouldBe Sleeping
    }

    "schedule perform database insert" in {
      // Arrange
      val messageDispatcher = TestProbe("message-distributor")
      val telegram = TestSupport.randomTelegram()

      // Act
      val fsm = TestFSMRef(new DailyHistoryActor(messageDispatcher.ref, settings), "schedule-database-insert")
      val currentState = fsm.stateName
      messageDispatcher.send(fsm, StoreMeterReading(MeterReading(LocalDate.now(), BigDecimal(3), BigDecimal(42), BigDecimal(16))))

      // Assert
      fsm.stateName shouldBe currentState
    }

    "wake up after resolution time" in {
      // Arrange
      val messageDispatcher = TestProbe("message-distributor")

      // Act
      val fsm = TestFSMRef(new DailyHistoryActor(messageDispatcher.ref, settings), "daily-wake-up")
      fsm.setState(Sleeping, Empty)
      fsm ! StateTimeout // Since sleep time is 1 day, we need to simulate it's time to wake up

      // Assert
      fsm.stateName shouldBe Receiving
    }

    "store telegrams in database" in {
      val messageDispatcher = TestProbe("message-distributor")
      val telegram = TestSupport.randomTelegram()
      val history = RingBuffer[P1Telegram](2)

      // Act
      val fsm = TestFSMRef(new RecentHistoryActor(messageDispatcher.ref, settings), "recent-store")
      messageDispatcher.send(fsm, TelegramReceived(telegram))

      // Assert

    }
  }
}
