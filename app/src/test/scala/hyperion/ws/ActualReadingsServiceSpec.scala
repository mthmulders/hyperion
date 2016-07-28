package hyperion.ws

import akka.testkit.TestProbe
import hyperion.rest.HyperionJsonProtocol
import hyperion.BaseSpec
import hyperion.MessageDistributor.RegisterReceiver
import spray.testkit.ScalatestRouteTest

class ActualReadingsServiceSpec extends BaseSpec with ScalatestRouteTest with HyperionJsonProtocol {
  "The ActualReadings WebSocket API" should {
    "should link the client to the message distributor" in {
      // Arrange
      val messageDistributor = TestProbe()
      val route = new ActualReadingsService(messageDistributor.ref, system).route

        // Act
      Get("/actual") ~> route

      // Assert
      messageDistributor.expectMsg(RegisterReceiver)
    }
  }
}