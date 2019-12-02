package hyperion

import java.util.concurrent.atomic.AtomicInteger

import io.restassured.RestAssured
import io.restassured.config.JsonConfig.jsonConfig
import io.restassured.path.json.config.JsonPathConfig.NumberReturnType._
import io.restassured.config.RestAssuredConfig
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest._
import org.slf4j.LoggerFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.{ AnyWordSpec, AnyWordSpecLike }

private object BaseIntegrationSpec {
  private val port = new AtomicInteger(8080)
}

abstract class BaseIntegrationSpec
  extends AnyWordSpec
    with Matchers
    with TypeCheckedTripleEquals
    with Inspectors
    with AnyWordSpecLike
    with BeforeAndAfterAll
    with BeforeAndAfterEach {

  RestAssured.config = RestAssuredConfig.config()
    .jsonConfig(jsonConfig().numberReturnType(FLOAT_AND_DOUBLE))

  private[this] val logger = LoggerFactory.getLogger(getClass)
  protected[this] var app: IntegrationTestApp = _
  protected[this] val port: Int = BaseIntegrationSpec.port.getAndIncrement

  override def beforeAll(): Unit = {
    logger.info(s"Starting new instance of Hyperion for ${getClass.getSimpleName} at port $port")
    app = IntegrationTestApp(port)
    io.restassured.RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
  }

  override def afterAll(): Unit = {
    logger.info("Stopping instance of Hyperion")
    app.stop()
  }
}