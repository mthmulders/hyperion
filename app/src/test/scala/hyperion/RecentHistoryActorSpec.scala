package hyperion

import akka.testkit.{TestFSMRef, TestProbe}
import hyperion.MessageDistributor.RegisterReceiver
import hyperion.RecentHistoryActor.{GetRecentHistory, History, Receiving, RecentReadings, Sleeping}
import hyperion.p1.{P1Telegram, TelegramReceived}
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.Eventually
import org.scalatest.time.SpanSugar

class RecentHistoryActorSpec extends BaseAkkaSpec with Eventually with OneInstancePerTest with SpanSugar {
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
      eventually(timeout(5 seconds), interval(500 millis)) {
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
      eventually(timeout(5 seconds), interval(500 millis)) {
        rha.stateName shouldBe Receiving
      }
    }

    "store telegrams in memory" in {
      // Arrange
      val telegram = TestSupport.randomTelegram()

      // Act
      rha ! TelegramReceived(telegram)

      // Assert
      eventually(timeout(5 seconds), interval(500 millis)) {
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
      eventually(timeout(5 seconds), interval(500 millis)) {
        val result = client.expectMsgClass(classOf[RecentReadings])
        result.telegrams.length should (be > 0 and be <= 10)
      }
    }
  }
}
