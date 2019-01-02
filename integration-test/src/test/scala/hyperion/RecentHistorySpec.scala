package hyperion

import hyperion.p1.TelegramReceived
import io.restassured.RestAssured.given
import org.hamcrest.Matchers._

class RecentHistorySpec extends BaseIntegrationSpec {
  "The Recent History API" should {
    "expose recent meter readings" should {
      "return the last reading received by the system" in {
        app.recentHistoryActor ! TelegramReceived(TestSupport.randomTelegram())

        given()
          .port(port).

        when().
          get("/recent").


        `then`().
          statusCode(200).
          body("size()", equalTo(1))
      }
    }
  }
}
