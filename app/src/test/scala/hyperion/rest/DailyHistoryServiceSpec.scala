package hyperion.rest

import java.time.LocalDate

import akka.actor.ActorRef
import akka.testkit.TestActor.AutoPilot
import akka.testkit.{TestActor, TestProbe}
import hyperion.BaseSpec
import hyperion.DailyHistoryActor.{RetrieveMeterReading, RetrievedMeterReading}
import hyperion.database.MeterReadingDAO.HistoricalMeterReading
import spray.http.StatusCodes
import spray.routing.HttpService
import spray.testkit.ScalatestRouteTest

import scala.util.Random

class DailyHistoryServiceSpec extends BaseSpec with ScalatestRouteTest with HyperionJsonProtocol with HttpService {
  def actorRefFactory = system

  private def dailyHistoryService(dailyHistoryActor: ActorRef) = {
    new DailyHistoryService(dailyHistoryActor).route
  }

  "The Daily History REST API" should {
    "return a recent meter reading by date" in {
      // Arrange
      val today = LocalDate.now()
      val dailyHistoryActor = TestProbe()
      val meterReading = HistoricalMeterReading(today, Random.nextDouble(), Random.nextDouble(), Random.nextDouble())
      dailyHistoryActor.setAutoPilot(new AutoPilot {
        override def run(sender: ActorRef, msg: Any): AutoPilot = {
          msg match {
            case RetrieveMeterReading(date) if today.isEqual(date) => sender ! RetrievedMeterReading(Some(meterReading));
            case RetrieveMeterReading(date) if !today.isEqual(date) => fail(s"RetrieveMeterReading has wrong date: $date. Expected $today")
            case msg: Any => fail(s"Didn't expect a $msg")
          }
          noAutoPilot
        }
      })
      val route = dailyHistoryService(dailyHistoryActor.ref)

      // Act
      Get(s"/history?date=$today") ~> route ~> check {
        // Assert
        dailyHistoryActor.expectMsgAllClassOf(classOf[RetrieveMeterReading]).map(_.date shouldBe today)

        status shouldBe StatusCodes.OK

        entity.asString should include(s""""recordDate": "$today",""")
      }
    }

    "return HTTP 404 when there is no meter reading for the requested date" in {
      // Arrange
      val today = LocalDate.now()
      val dailyHistoryActor = TestProbe()
      dailyHistoryActor.setAutoPilot(new AutoPilot {
        override def run(sender: ActorRef, msg: Any): AutoPilot = {
          msg match {
            case RetrieveMeterReading(date) if today.isEqual(date) => sender ! RetrievedMeterReading(None);
            case RetrieveMeterReading(date) if !today.isEqual(date) => fail(s"RetrieveMeterReading has wrong date: $date. Expected $today")
            case msg: Any => fail(s"Didn't expect a $msg")
          }
          noAutoPilot
        }
      })
      val route = dailyHistoryService(dailyHistoryActor.ref)

      // Act
      Get(s"/history?date=$today") ~> route ~> check {
        // Assert
        dailyHistoryActor.expectMsgAllClassOf(classOf[RetrieveMeterReading]).map(_.date shouldBe today)

        status shouldBe StatusCodes.NotFound

        entity.asString shouldBe s"No record found for date $today"
      }
    }

    "return HTTP 400 when the request parameter cannot be parsed as a date" in {
      // Arrange
      val dailyHistoryActor = TestProbe()
      val route = dailyHistoryService(dailyHistoryActor.ref)

      // Act
      Get("/history?date=garbage") ~> sealRoute(route) ~> check {
        // Assert
        dailyHistoryActor.expectNoMsg()
        status shouldBe StatusCodes.BadRequest
      }
    }
  }
}
