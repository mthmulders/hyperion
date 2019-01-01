package hyperion

import io.restassured.RestAssured.when
import io.restassured.module.scala.RestAssuredSupport.AddThenToResponse
import org.hamcrest.Matchers._

class AppInfoSpec extends BaseIntegrationSpec {
  "The App info API" should {
    "expose database information" in {
      when().
        get("/info").

      Then().
        statusCode(200).
        body("database", containsString("PostgreSQL"))
    }

    "expose Java information" in {
      val javaVersion = System.getProperty("java.version")
      val javaVendor = System.getProperty("java.vendor")

      when().
        get("/info").

      Then().
        statusCode(200).
        body("javaVersion", containsString(javaVersion)).
        body("javaVersion", containsString(javaVendor))
    }

    "expose Scala information" in {
      when().
        get("/info").

      Then().
        statusCode(200).
        body("scalaVersion", containsString(BuildInfo.scalaVersion))
    }

    "expose application version" in {
      when().
        get("/info").

      Then().
        statusCode(200).
        body("appVersion", containsString(BuildInfo.version))
    }
  }
}
