package hyperion

import akka.testkit.{TestFSMRef, TestProbe}
import hyperion.MessageDistributor.RegisterReceiver
import hyperion.RecentHistoryActor.{GetRecentHistory, History, Receiving, RecentReadings, Sleeping}
import hyperion.p1.{P1Telegram, TelegramReceived}
import org.scalatest.concurrent.Eventually

class RecentHistoryActorSpec extends BaseAkkaSpec with Eventually {
  private val messageDistributor: TestProbe = TestProbe("message-distributor")

  private val rha = TestFSMRef(new RecentHistoryActor(messageDistributor.ref), "recent-history-actor")

  "The Recent History Actor" should {
    "register itself with the Message Distributor" in {
      // Assert
      messageDistributor.expectMsg(RegisterReceiver)
    }

    "go to sleep after having received one Telegram" in {
      // Arrange
      val telegram = TestSupport.randomTelegram()

      // Act
      rha ! TelegramReceived(telegram)

      // Assert
      eventually {
        rha.stateName shouldBe Sleeping
      }
    }

    "wake up after resolution time" in {
      // Arrange
      // Sleep time is set in src/test/resources/application.conf: 100 millis
      val history = RingBuffer[P1Telegram](2)

      // Act
      rha.setState(Sleeping, History(history))

      // Assert
      eventually {
        rha.stateName shouldBe Receiving
      }
    }

    "store telegrams in memory" in {
      // Arrange
      val telegram = TestSupport.randomTelegram()

      // Act
      rha ! TelegramReceived(telegram)

      // Assert
      eventually {
        rha.stateData shouldBe an[History]
        rha.stateData.asInstanceOf[History].telegrams.length shouldBe 1
      }
    }

    "return all recent readings when asked" in {
      // Arrange
      val client = TestProbe()
      val telegram = TestSupport.randomTelegram()

      // Act
      rha ! TelegramReceived(telegram)
      client.send(rha, GetRecentHistory)

      // Assert
      eventually {
        val result = client.expectMsgClass(classOf[RecentReadings])
        result.telegrams.length should (be > 0 and be <= 10)
      }
    }
  }
}
