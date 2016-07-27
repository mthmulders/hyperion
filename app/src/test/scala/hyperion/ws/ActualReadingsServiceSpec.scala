package hyperion.ws

import akka.actor.ActorRef
import akka.testkit.TestActor.AutoPilot
import akka.testkit.{TestActor, TestProbe}
import hyperion.rest.HyperionJsonProtocol
import hyperion.BaseSpec
import spray.http.HttpRequest
import spray.testkit.ScalatestRouteTest

class ActualReadingsServiceSpec extends BaseSpec with ScalatestRouteTest with HyperionJsonProtocol {
  "The ActualReadings WebSocket API" should {
    "delegate to upgrade the connection to a WebSocket" in {
      // Arrange
      val connectionUpgrade = new Object // real type doesn't matter here
      val messageDistributor = TestProbe()
      messageDistributor.setAutoPilot(new AutoPilot {
        override def run(sender: ActorRef, msg: Any): AutoPilot = {
          msg match {
            case HttpRequest => sender ! connectionUpgrade
            case m => logger.warn(s"Unexecpted message $m")
          }
          TestActor.KeepRunning
        }
      })
      val route = new ActualReadingsService(messageDistributor.ref, system).route

        // Act
      Get("/actual") ~> route ~> check {
        // Assert
      }
    }
  }
}