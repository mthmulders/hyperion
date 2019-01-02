package hyperion

import io.restassured.RestAssured.given
import org.hamcrest.Matchers._

class AppInfoSpec extends BaseIntegrationSpec {
  "The App info API" should {
    "expose database information" in {
      given()
        .port(port).

      when().
        get("/info").

      `then`().
        statusCode(200).
        body("database", containsString("PostgreSQL"))
    }

    "expose Java information" in {
      val javaVersion = System.getProperty("java.version")
      val javaVendor = System.getProperty("java.vendor")

      given()
        .port(port).

      when().
        get("/info").

      `then`().
        statusCode(200).
        body("javaVersion", containsString(javaVersion)).
        body("javaVersion", containsString(javaVendor))
    }

    "expose Scala information" in {
      given()
        .port(port).

      when().
        get("/info").

      `then`().
        statusCode(200).
        body("scalaVersion", containsString(BuildInfo.scalaVersion))
    }

    "expose application version" in {
      given()
        .port(port).

      when().
        get("/info").

      `then`().
        statusCode(200).
        body("appVersion", containsString(BuildInfo.version))
    }
  }
}
