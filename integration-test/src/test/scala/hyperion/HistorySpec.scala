package hyperion

import io.restassured.RestAssured.given
import org.hamcrest.Matchers._

class HistorySpec extends BaseIntegrationSpec {
  "The History API" should {
    "expose meter readings by date" should {
      "return meter reading if data is present" in {
        given().
          port(port).

        when().
          get("/history?date=2017-01-01").

        `then`().
          statusCode(200).
          body("recordDate", equalTo("2017-01-01")).
          body("gas", is(1.5f)).
          body("electricityNormal", equalTo(2.6f)).
          body("electricityLow", equalTo(3.7f))
      }

      "return \'Not Found\' if no data is present" in {
        given().
          port(port).

        when().
          get("/history?date=2016-12-31").

        `then`().
          statusCode(404)
      }
    }

    "expose meter readings by month" should {
      "return meter reading if data is present" in {
        given().
          port(port).

        when().
          get("/history?month=01&year=2017").

        `then`().
          statusCode(200).
          body("[0].recordDate", equalTo("2017-01-01")).
          body("[0].gas", is(1.5f)).
          body("[0].electricityNormal", equalTo(2.6f)).
          body("[0].electricityLow", equalTo(3.7f))
      }

      "return \'Not Found\' if no data is present" in {
        given().
          port(port).

        when().
          get("/history?month=12&year=2016").

        `then`().
          statusCode(404)
      }
    }
  }
}