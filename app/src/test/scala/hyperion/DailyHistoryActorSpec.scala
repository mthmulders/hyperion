package hyperion

import java.time.LocalDate

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

import akka.actor.FSM.StateTimeout
import akka.testkit.{TestFSMRef, TestProbe}
import akka.util.Timeout
import org.scalatest.concurrent.ScalaFutures

import hyperion.MessageDistributor.RegisterReceiver
import hyperion.DailyHistoryActor._
import hyperion.database.DatabaseActor.StoreMeterReading
import hyperion.p1.TelegramReceived

class DailyHistoryActorSpec extends BaseAkkaSpec with ScalaFutures {
  private implicit val timeout: Timeout = Timeout(500 milliseconds)
  private val messageDistributor = TestProbe("message-distributor")
  private val databaseActor = TestProbe("database")

  private val dha = TestFSMRef(new DailyHistoryActor(messageDistributor.ref, databaseActor.ref), "daily-history-actor")

  "The Daily History Actor" should {
    "register itself with the Message Distributor" in {
      // Assert
      messageDistributor.expectMsg(RegisterReceiver)
    }

    "after having received one Telegram" should {
      "schedule to wake up" in {
        // Arrange
        val telegram = TestSupport.randomTelegram()

        // Act
        messageDistributor.send(dha, TelegramReceived(telegram))

        // Assert
        dha.isTimerActive("wake-up") shouldBe true
      }

      "go to sleep" in {
        // Arrange
        val telegram = TestSupport.randomTelegram()

        // Act
        messageDistributor.send(dha, TelegramReceived(telegram))

        // Assert
        dha.stateName shouldBe Sleeping
      }

      "schedule to perform database insert" in {
        // Arrange
        val telegram = TestSupport.randomTelegram()
        dha.setState(Receiving, Empty)

        // Act
        messageDistributor.send(dha, TelegramReceived(telegram))

        // Assert
        val reading = (databaseActor expectMsgAllClassOf classOf[StoreMeterReading]).map(_.reading)
        reading.map(_.recordDate shouldBe LocalDate.now())
      }
    }

    "wake up after resolution time" in {
      // Arrange
      dha.setState(Sleeping, Empty)

      // Act
      dha ! StateTimeout // Since sleep time is typically 1 day, we need to simulate it's time to wake up

      // Assert
      dha.stateName shouldBe Receiving
    }
  }
}
