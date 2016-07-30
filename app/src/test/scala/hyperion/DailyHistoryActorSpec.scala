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
import org.mockito.Mockito.verify
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar

class DailyHistoryActorSpec extends BaseAkkaSpec with MockitoSugar with ScalaFutures {
  var meterReadingDAO: MeterReadingDAO = _
  implicit val timeout = Timeout(500 milliseconds)

  override def beforeEach = {
    log.info("Creating new MeterReadingDAO mock")
    meterReadingDAO = mock[MeterReadingDAO]
  }

  "The Daily History Actor" should {
    "register itself with the Message Distributor" in {
      // Arrange
      val messageDistributor = TestProbe("recemessage-distributor")

      // Act
      system.actorOf(Props(new DailyHistoryActor(messageDistributor.ref, meterReadingDAO, settings)), "daily-register")

      // Assert
      messageDistributor.expectMsg(RegisterReceiver)
    }

    "go to sleep after having received one Telegram" in {
      // Arrange
      val messageDispatcher = TestProbe("message-distributor")
      val telegram = TestSupport.randomTelegram()

      // Act
      val fsm = TestFSMRef(new DailyHistoryActor(messageDispatcher.ref, meterReadingDAO, settings), "daily-go-to-sleep")
      messageDispatcher.send(fsm, TelegramReceived(telegram))

      // Assert
      fsm.stateName shouldBe Sleeping
    }

    "schedule perform database insert" in {
      // Arrange
      val messageDispatcher = TestProbe("message-distributor")
      val telegram = TestSupport.randomTelegram()

      // Act
      val fsm = TestFSMRef(new DailyHistoryActor(messageDispatcher.ref, meterReadingDAO, settings), "schedule-database-insert")
      val currentState = fsm.stateName
      messageDispatcher.send(fsm, StoreMeterReading(HistoricalMeterReading(LocalDate.now(), BigDecimal(3), BigDecimal(42), BigDecimal(16))))

      // Assert
      fsm.stateName shouldBe currentState
    }

    "wake up after resolution time" in {
      // Arrange
      val messageDispatcher = TestProbe("message-distributor")

      // Act
      val fsm = TestFSMRef(new DailyHistoryActor(messageDispatcher.ref, meterReadingDAO, settings), "daily-wake-up")
      fsm.setState(Sleeping, Empty)
      fsm ! StateTimeout // Since sleep time is 1 day, we need to simulate it's time to wake up

      // Assert
      fsm.stateName shouldBe Receiving
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
  private def storeTelegramsInDatabase(state: State) = {
    // Arrange
    val messageDispatcher = TestProbe("message-distributor")
    val reading = HistoricalMeterReading(LocalDate.now(), Random.nextDouble(), Random.nextDouble(), Random.nextDouble())

    // Act
    val fsm = TestFSMRef(new DailyHistoryActor(messageDispatcher.ref, meterReadingDAO, settings), s"$state-daily-store")
    fsm.setState(state)
    messageDispatcher.send(fsm, StoreMeterReading(reading))

    // Assert
    within(1 second) {
      verify(meterReadingDAO).recordMeterReading(reading)
    }
  }

  private def retrieveMeterReadingsFromDatabase(state: State) = {
    // Arrange
    val date = LocalDate.now()
    val result = Seq(HistoricalMeterReading(LocalDate.now(), Random.nextDouble(), Random.nextDouble(), Random.nextDouble()))
    when(meterReadingDAO.retrieveMeterReading(date)).thenReturn(Future { result })

    // Act
    val fsm = TestFSMRef(new DailyHistoryActor(TestProbe().ref, meterReadingDAO, settings), s"$state-daily-retrieve")
    fsm.setState(state)
    val future = fsm ? RetrieveMeterReading(date)

    // Assert
    whenReady(future) { answer =>
      answer shouldBe an[RetrievedMeterReading]
      answer.asInstanceOf[RetrievedMeterReading].reading shouldBe result.headOption
    }
  }
}
