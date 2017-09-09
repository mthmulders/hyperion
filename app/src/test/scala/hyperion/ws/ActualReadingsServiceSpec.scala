package hyperion.ws

import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import akka.testkit.TestProbe

import hyperion.rest.HyperionJsonProtocol
import hyperion.BaseSpec

class ActualReadingsServiceSpec extends BaseSpec with ScalatestRouteTest with HyperionJsonProtocol {
  private val messageDistributor = TestProbe()
  private val route = new ActualReadingsService(messageDistributor.ref, system).route

  "The ActualReadings service" should {
    "upgrade the connection to a WebSocket" in {
      // Arrange
      val wsClient = WSProbe()

      // Act
      WS("/actual", wsClient.flow) ~> route ~>
        check {
          // Assert
          isWebSocketUpgrade shouldEqual true
        }
    }
  }
}