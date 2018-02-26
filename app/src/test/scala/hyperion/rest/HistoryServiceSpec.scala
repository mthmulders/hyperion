package hyperion.rest

import java.time.LocalDate

import scala.collection.immutable.Seq
import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route.seal
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.model.HttpEntity.Empty
import akka.testkit.TestActor.AutoPilot
import akka.testkit.TestProbe
import hyperion.BaseSpec
import hyperion.database.DatabaseActor.{RetrieveMeterReadingForDate, RetrieveMeterReadingForMonth, RetrievedMeterReadings}
import hyperion.database.HistoricalMeterReading

class HistoryServiceSpec extends BaseSpec with ScalatestRouteTest with HyperionJsonProtocol {
  private val today = LocalDate.now()
  private val meterReading = HistoricalMeterReading(today, BigDecimal(1), BigDecimal(2), BigDecimal(3))

  private val databaseActor = TestProbe()

  databaseActor.setAutoPilot(new AutoPilot {
    override def run(sender: ActorRef, msg: Any): AutoPilot = {
      msg match {
        case RetrieveMeterReadingForMonth(month, year) if month.equals(today.getMonth) && year.equals(today.getYear) =>
          sender ! RetrievedMeterReadings(Seq(meterReading))
        case RetrieveMeterReadingForDate(date) if today.isEqual(date) =>
          sender ! RetrievedMeterReadings(Seq(meterReading));
        case RetrieveMeterReadingForDate(_) =>
          sender ! RetrievedMeterReadings(Seq.empty);
      }
      keepRunning
    }
  })

  private val route = new HistoryService(databaseActor.ref).route

  "The Daily History REST API" should {
    "query the database" in {
      // Act
      Get(s"/history?date=$today") ~> route ~> check {
        // Assert
        databaseActor.expectMsgAllClassOf(classOf[RetrieveMeterReadingForDate]).map(_.date shouldBe today)
      }
    }

    "return a recent meter reading by date" in {
      // Act
      Get(s"/history?date=$today") ~> route ~> check {
        // Assert
        status shouldBe StatusCodes.OK
        responseAs[String] should startWith("{")
        responseAs[String] should include(s""""recordDate":"$today",""")
        responseAs[String] should endWith("}")
      }
    }

    "return recent meter readings by month" in {
      // Act
      Get(s"/history?month=${today.getMonthValue}&year=${today.getYear}") ~> route ~> check {
        // Assert
        status shouldBe StatusCodes.OK
        responseAs[String] should startWith("[")
        responseAs[String] should include(s""""recordDate":"$today",""")
        responseAs[String] should endWith("]")
      }
    }

    "return 'Not Found' when there is no record for the requested date" in {
      val yesterday = today.minusDays(1)

      // Act
      Get(s"/history?date=$yesterday") ~> route ~> check {
        // Assert
        status shouldBe StatusCodes.NotFound
        responseEntity shouldBe Empty
      }
    }

    "return 'Bad Request' when the request parameter cannot be parsed as a date" in {
      // Act
      Get("/history?date=garbage") ~> seal(route) ~> check {
        // Assert
        status shouldBe StatusCodes.BadRequest
      }
    }
  }
}
