package hyperion.database

import java.time.LocalDate

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
  private val meterReadingDAO = stub[MeterReadingDAO]

  private val da = system.actorOf(Props(new DatabaseActor(meterReadingDAO)), "database-actor")

  "The database actor" should {
    "store new meter readings in the database" in {
      // Arrange
      val reading = HistoricalMeterReading(LocalDate.now(), BigDecimal(1), BigDecimal(2), BigDecimal(3))

      // Act
      da ! StoreMeterReading(reading)

      // Assert
      (meterReadingDAO.recordMeterReading _).verify(reading)
    }

    "retrieve meter readings from the database" should {
      "by exact date" in {
        // Arrange
        val date = LocalDate.now()
        val result = Seq(HistoricalMeterReading(LocalDate.now(), BigDecimal(1), BigDecimal(2), BigDecimal(3)))
        (meterReadingDAO.retrieveMeterReading _).when(date).returns(Future { result })

        // Act
        whenReady((da ? RetrieveMeterReadingForDate(date)).mapTo[RetrievedMeterReading]) { answer =>
          // Assert
          answer shouldBe an[RetrievedMeterReading]
          answer.reading shouldBe result.headOption
        }
      }
    }
  }
}
