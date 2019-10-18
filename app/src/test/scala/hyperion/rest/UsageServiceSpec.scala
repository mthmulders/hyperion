package hyperion.rest

import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestActor.AutoPilot
import akka.testkit.TestProbe
import akka.util.Timeout
import hyperion.BaseSpec
import hyperion.UsageCalculationActor.{CalculateUsage, UsageDataRecord}
import hyperion.database.DatabaseActor._
import hyperion.database.HistoricalMeterReading
import org.slf4j.LoggerFactory

import scala.collection.immutable.Seq

class UsageServiceSpec extends BaseSpec with ScalatestRouteTest with HyperionJsonProtocol {

  private val log = LoggerFactory.getLogger(getClass)

  private val today = LocalDate.now()
  private val firstDayOfCurrentMonth = today.withDayOfMonth(1)
  private val firstDayOfNextMonth = today.plusMonths(1).withDayOfMonth(1)

  private val usageDataRecords = (0L until DAYS.between(firstDayOfCurrentMonth, firstDayOfNextMonth))
    .map(day => firstDayOfCurrentMonth.plusDays(day))
    .map(date => UsageDataRecord(date, 0, 0, 0))

  private val databaseActor = TestProbe()

  databaseActor.setAutoPilot(new AutoPilot {
    override def run(sender: ActorRef, msg: Any): AutoPilot = {
      msg match {
        case CalculateUsage(_ @`firstDayOfCurrentMonth`, _ @`firstDayOfNextMonth`) =>
          sender ! usageDataRecords
        case CalculateUsage(_, _) =>
          sender ! Seq.empty
      }
      keepRunning
    }
  })

  private val route = new UsageService(databaseActor.ref).route

  "The Usage Data REST API" should {

    "calculate usage data for each date in the range" in {
      val month = today.getMonthValue
      val year = today.getYear

      // Act
      Get(s"/usage?month=$month&year=$year") ~> route ~> check {
        // Assert
        status shouldBe StatusCodes.OK
        responseAs[String] should startWith("[")
        responseAs[String] should include(s""""date":"$today",""")
        responseAs[String] should endWith("]")
      }
    }
  }

}
