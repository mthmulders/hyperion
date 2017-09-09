package hyperion.rest

import scala.collection.immutable

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestActor.AutoPilot
import akka.testkit.TestProbe

import hyperion.{BaseSpec, TestSupport}
import hyperion.RecentHistoryActor.{GetRecentHistory, RecentReadings}

class RecentReadingsServiceSpec extends BaseSpec with ScalatestRouteTest with HyperionJsonProtocol {
  private val telegram = TestSupport.randomTelegram()
  private val recentHistoryActor = TestProbe()
  recentHistoryActor.setAutoPilot(new AutoPilot {
    override def run(sender: ActorRef, msg: Any): AutoPilot = {
      msg match {
        case GetRecentHistory => sender ! RecentReadings(immutable.Vector(telegram))
        case m => logger.warn(s"Unexecpted message $m")
      }
      keepRunning
    }
  })
  private val route = new RecentReadingsService(recentHistoryActor.ref).route

  "The RecentReadings REST API" should {
    "return the most recent meter readings in JSON" in {
      // Act
      Get("/recent") ~> route ~> check {
        // Assert
        status shouldBe StatusCodes.OK

        responseAs[String] should include(s""""tariff":"${telegram.data.currentTariff}"""")
      }
    }
  }
}