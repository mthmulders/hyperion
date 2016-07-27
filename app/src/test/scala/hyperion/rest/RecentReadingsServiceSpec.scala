package hyperion.rest

import akka.actor.ActorRef
import akka.testkit.TestActor.AutoPilot

import scala.collection.immutable
import akka.testkit.{TestActor, TestProbe}
import hyperion.{BaseSpec, TestSupport}
import hyperion.RecentHistoryActor.{GetRecentHistory, RecentReadings}
import spray.http.{HttpEntity, StatusCodes}
import spray.testkit.ScalatestRouteTest

class RecentReadingsServiceSpec extends BaseSpec with ScalatestRouteTest with HyperionJsonProtocol {

  "The RecentReadings REST API" should {
    "return the most recent meter readings in JSON" in {
      // Arrange
      val telegram = TestSupport.randomTelegram()
      val history = immutable.Vector(telegram)
      val recentHistoryActor = TestProbe()
      recentHistoryActor.setAutoPilot(new AutoPilot {
        override def run(sender: ActorRef, msg: Any): AutoPilot = {
          msg match {
            case GetRecentHistory => sender ! RecentReadings(history)
            case m => logger.warn(s"Unexecpted message $m")
          }
          TestActor.KeepRunning
        }
      })

      val route = new RecentReadingsService(recentHistoryActor.ref).route

      // Act
      Get("/recent") ~> route ~> check {
        // Assert
        status === StatusCodes.OK

        entity !== HttpEntity.Empty
        entity.toString should include(s""""tariff": "${telegram.data.currentTariff}"""")
      }
    }
  }
}