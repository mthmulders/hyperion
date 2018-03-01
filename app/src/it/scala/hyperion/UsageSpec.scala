package hyperion

import io.restassured.RestAssured.when
import io.restassured.module.scala.RestAssuredSupport.AddThenToResponse
import org.hamcrest.Matchers._

class UsageSpec extends BaseIntegrationSpec {
  "The Usage API" should {
    "expose usage data by month" should {
      "return \'Not Found\' if no data is present" in {
        when().
          get("/usage?month=12&year=2016").

        Then().
          statusCode(404)
      }

      "return meter reading if data is present" in {
        when().
          get("/usage?month=01&year=2017").

        Then().
          statusCode(200).

          body("[0].date", equalTo("2017-01-01")).
          body("[0].gas", is(0f)).
          body("[0].electricityNormal", equalTo(0.1f)).
          body("[0].electricityLow", equalTo(0f))
      }
    }
  }
}
