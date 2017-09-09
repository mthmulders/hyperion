package hyperion

import java.time.LocalDate

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.Random
import akka.actor.FSM.StateTimeout
import akka.actor.Props
import akka.pattern.ask
import akka.testkit.{TestFSMRef, TestProbe}
import akka.util.Timeout
import hyperion.MessageDistributor.RegisterReceiver
import hyperion.DailyHistoryActor._
import hyperion.database.MeterReadingDAO
import hyperion.database.MeterReadingDAO.HistoricalMeterReading
import org.scalamock.scalatest.MockFactory
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.ScalaFutures

class DailyHistoryActorSpec extends BaseAkkaSpec with OneInstancePerTest with MockFactory with ScalaFutures {
  private val meterReadingDAO: MeterReadingDAO = mock[MeterReadingDAO]
  private implicit val timeout: Timeout = Timeout(500 milliseconds)
  private val messageDistributor = TestProbe("message-distributor")

  private val dha = TestFSMRef(new DailyHistoryActor(messageDistributor.ref, meterReadingDAO, settings), "daily-history-actor")

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
    }

    "schedule to perform database insert" in {
      // Arrange
      val telegram = TestSupport.randomTelegram()
      (meterReadingDAO.recordMeterReading _).expects(*).returns(Future {})
      val msg = StoreMeterReading(HistoricalMeterReading(LocalDate.now(), BigDecimal(3), BigDecimal(42), BigDecimal(16)))

      // Act
      val currentState = dha.stateName
      messageDistributor.send(dha, msg)

      // Assert
      dha.stateName shouldBe currentState
    }

    "wake up after resolution time" in {
      // Arrange

      // Act
      dha.setState(Sleeping, Empty)
      dha ! StateTimeout // Since sleep time is typically 1 day, we need to simulate it's time to wake up

      // Assert
      dha.stateName shouldBe Receiving
    }

    "while sleeping" should {
      "retrieve existing meter readings from database" in {
        retrieveMeterReadingsFromDatabase(Sleeping)
      }

      "store telegrams in database" in {
        storeTelegramsInDatabase(Sleeping)
      }
    }

    "while awake" should {
      "retrieve readings from database" in {
        retrieveMeterReadingsFromDatabase(Receiving)
      }

      "store telegrams in database" in {
        storeTelegramsInDatabase(Receiving)
      }
    }
  }

  // Re-usabe testcases that need to be ran in multiple states
  private val storeTelegramsInDatabase = (state: State) => {
    // Arrange
    val reading = HistoricalMeterReading(LocalDate.now(), Random.nextDouble(), Random.nextDouble(), Random.nextDouble())

    // Assert
    (meterReadingDAO.recordMeterReading _).expects(reading).returns(Future {})

    // Act
    dha.setState(state)
    messageDistributor.send(dha, StoreMeterReading(reading))
  }

  private val retrieveMeterReadingsFromDatabase = (state: State) => {
    // Arrange
    val date = LocalDate.now()
    val result = Seq(HistoricalMeterReading(LocalDate.now(), Random.nextDouble(), Random.nextDouble(), Random.nextDouble()))
    (meterReadingDAO.retrieveMeterReading _).expects(date).returns(Future { result })

    // Act
    dha.setState(state)
    val future = dha ? RetrieveMeterReading(date)

    // Assert
    whenReady(future) { answer =>
      answer shouldBe an[RetrievedMeterReading]
      answer.asInstanceOf[RetrievedMeterReading].reading shouldBe result.headOption
    }
  }
}
