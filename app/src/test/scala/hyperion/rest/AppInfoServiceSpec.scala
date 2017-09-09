package hyperion.rest

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest

import hyperion.BuildInfo
import hyperion.BaseSpec

class AppInfoServiceSpec extends BaseSpec with ScalatestRouteTest with HyperionJsonProtocol {
  "The Application Info REST API" should {
    "return some application info" in {
      // Arrange
      val route = new AppInfoService().route

      // Act
      Get("/info") ~> route ~> check {
        // Assert
        status shouldBe StatusCodes.OK
        responseAs[String] should include(s""""appVersion":"${BuildInfo.version}"""")
        responseAs[String] should include(s""""scalaVersion":"${BuildInfo.scalaVersion}"""")
        responseAs[String] should include(s""""javaVersion":"""")
      }
    }
  }
}
