package hyperion.database

import java.time.LocalDate

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout
import org.scalamock.scalatest.MockFactory
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.ScalaFutures

import hyperion.BaseAkkaSpec
import hyperion.database.DatabaseActor._

class DatabaseActorSpec extends BaseAkkaSpec with OneInstancePerTest with MockFactory with ScalaFutures {
  private implicit val timeout: Timeout = Timeout(500 milliseconds)

  private def dateRange(start: LocalDate, end: LocalDate): Seq[LocalDate] =
    Iterator.iterate(start)(_.plusDays(1)).takeWhile(!_.isAfter(end)).to[Seq]

  private val meterReadingDAO = stub[MeterReadingDAO]
  private val da = system.actorOf(Props(new DatabaseActor() {
    protected override def createDao() = meterReadingDAO
  }))

  "The database actor" should {
    "store new meter readings in the database" in {
      // Arrange
      val reading = HistoricalMeterReading(LocalDate.now(), BigDecimal(1), BigDecimal(2), BigDecimal(3))

      // Act
      da ! StoreMeterReading(reading)

      // Assert
      Thread.sleep(500)
      (meterReadingDAO.recordMeterReading _).verify(reading)
    }

    "retrieve meter readings from the database" should {
      "by exact date" in {
        // Arrange
        val date = LocalDate.now()
        val result = HistoricalMeterReading(LocalDate.now(), BigDecimal(1), BigDecimal(2), BigDecimal(3))
        (meterReadingDAO.retrieveMeterReading _).when(date).returns(Future { Some(result) })

        // Act
        whenReady((da ? RetrieveMeterReadingForDate(date)).mapTo[RetrievedMeterReadings]) { answer =>
          // Assert
          answer.readings should contain only result
        }
      }

      "by month" in {
        // Arrange
        val (month, year) = (LocalDate.now().getMonth, LocalDate.now().getYear)

        val startDate = LocalDate.of(year, month, 1)
        val endDate = startDate.plusMonths(1).minusDays(1)

        // Create a record for every day of the selected month
        val result = dateRange(startDate, endDate)
          .map(HistoricalMeterReading(_, BigDecimal(1), BigDecimal(2), BigDecimal(3)))

        (meterReadingDAO.retrieveMeterReadings _).when(startDate, endDate).returns(Future { result })

        // Act
        whenReady((da ? RetrieveMeterReadingForMonth(month, year)).mapTo[RetrievedMeterReadings]) { answer =>
          // Assert
          answer.readings should have length result.size
        }
      }

      "by date range" in {
        // Arrange
        val (month, year) = (LocalDate.now().getMonth, LocalDate.now().getYear)

        val startDate = LocalDate.of(year, month, 1)
        val endDate = startDate.plusMonths(1)

        // Create a record for every day of the selected month
        val result = dateRange(startDate, endDate)
          .map(HistoricalMeterReading(_, BigDecimal(1), BigDecimal(2), BigDecimal(3)))

        (meterReadingDAO.retrieveMeterReadings _).when(startDate, endDate).returns(Future { result })

        // Act
        whenReady((da ? RetrieveMeterReadingForDateRange(startDate, endDate)).mapTo[RetrievedMeterReadings]) { answer =>
          // Assert
          answer.readings should have length result.size
        }

      }
    }
  }
}
