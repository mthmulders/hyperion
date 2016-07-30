package hyperion.rest

import hyperion.BuildInfo
import hyperion.BaseSpec
import spray.http.StatusCodes
import spray.testkit.ScalatestRouteTest

class AppInfoServiceSpec extends BaseSpec with ScalatestRouteTest with HyperionJsonProtocol {
  "The Application Info REST API" should {
    "return some application info" in {
      // Arrange
      val route = new AppInfoService().route

      // Act
      Get("/info") ~> route ~> check {
        // Assert
        status shouldBe StatusCodes.OK
        entity.asString should include(s""""appVersion": "${BuildInfo.version}"""")
        entity.asString should include(s""""scalaVersion": "${BuildInfo.scalaVersion}"""")
        entity.asString should include(s""""javaVersion": """")

        logger.info(entity.asString)
      }
    }
  }
}
