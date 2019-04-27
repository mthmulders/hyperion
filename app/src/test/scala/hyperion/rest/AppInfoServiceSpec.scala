package hyperion.rest

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestActor.AutoPilot
import akka.testkit.TestProbe
import hyperion.BuildInfo
import hyperion.BaseSpec
import hyperion.database.DatabaseActor.GetDatabaseInfo

class AppInfoServiceSpec extends BaseSpec with ScalatestRouteTest with HyperionJsonProtocol {
  private val databaseName = "PostgreSQL 9.3.4"
  private val databaseActor = TestProbe()

  databaseActor.setAutoPilot(new AutoPilot {
    override def run(sender: ActorRef, msg: Any): AutoPilot = {
      msg match {
        case GetDatabaseInfo => sender ! databaseName
      }
      keepRunning
    }
  })

  "The Application Info REST API" should {
    "return some application info" in {
      // Arrange
      val route = new AppInfoService(databaseActor.ref).route

      // Act
      Get("/info") ~> route ~> check {
        // Assert
        status shouldBe StatusCodes.OK
        val response = responseAs[String]
        response should include(s""""appVersion":"${BuildInfo.version}"""")
        response should include(s""""scalaVersion":"${BuildInfo.scalaVersion}"""")
        response should include(s""""javaVersion":"""")
        response should include(s""""database":"$databaseName""")
      }
    }
  }
}
