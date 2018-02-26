package hyperion

import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS

import akka.actor.ActorRef
import akka.pattern.ask
import akka.testkit.TestActor.AutoPilot
import akka.testkit.TestProbe
import akka.util.Timeout
import hyperion.UsageCalculationActor.{CalculateUsage, UsageDataRecord}
import hyperion.database.DatabaseActor.{RetrieveMeterReadingForDateRange, RetrievedMeterReadings}
import hyperion.database.HistoricalMeterReading
import org.scalatest.concurrent.ScalaFutures

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class UsageCalculationActorSpec extends BaseAkkaSpec with ScalaFutures {
  private implicit val timeout: Timeout = Timeout(500 milliseconds)

  private val numberOfDays = 5
  private val firstDay = LocalDate.now()
  private val lastDay = firstDay.plusDays(numberOfDays)

  private val meterReadings = (0l until numberOfDays + 1)
    .map(day => HistoricalMeterReading(firstDay.plusDays(day), 0, day, 0))

  private val database = TestProbe("database")
  private val probe = TestProbe()

  database.setAutoPilot(new AutoPilot {
    override def run(sender: ActorRef, msg: Any): AutoPilot = {
      msg match {
        case RetrieveMeterReadingForDateRange(_ @`firstDay`, _ @`lastDay`) =>
          sender ! RetrievedMeterReadings(meterReadings)
      }
      keepRunning
    }
  })

  private val uca = system.actorOf(UsageCalculationActor.props(database.ref), "usage-calculation-actor")

  "The Usage Calculation Actor" should {
    "query the database" in {
      // Act
      probe.send(uca, CalculateUsage(firstDay, lastDay))

      // Assert
      database.expectMsgAllClassOf(classOf[RetrieveMeterReadingForDateRange]).map(msg => {
        msg.start shouldBe firstDay
        msg.end shouldBe lastDay
      })
    }

    "calculate the usage per date" in {
      // Arrange
      val numDays = DAYS.between(firstDay, lastDay)

      // Act
      val answer: Future[Any] = uca ? CalculateUsage(firstDay, lastDay)

      // Assert
      whenReady(answer.mapTo[Seq[UsageDataRecord]]) { records =>
        records.length shouldBe numDays
        // each day has electricityNormal one higher than the day before
        records.map(_.electNormal).sum shouldBe numberOfDays
//        records.foldLeft(BigDecimal(0))((sum, udr) => sum + udr.electNormal) shouldBe numberOfDays
      }
    }
  }
}
