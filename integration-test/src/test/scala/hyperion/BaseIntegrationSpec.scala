package hyperion

import java.util.concurrent.atomic.AtomicInteger

import io.restassured.RestAssured
import io.restassured.config.JsonConfig.jsonConfig
import io.restassured.path.json.config.JsonPathConfig.NumberReturnType._
import io.restassured.config.RestAssuredConfig
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest._
import org.slf4j.LoggerFactory

private object BaseIntegrationSpec {
  private val port = new AtomicInteger(8080)
}

abstract class BaseIntegrationSpec
  extends WordSpec
    with Matchers
    with TypeCheckedTripleEquals
    with Inspectors
    with WordSpecLike
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
  }

  override def afterAll(): Unit = {
    logger.info("Stopping instance of Hyperion")
    app.stop()
  }
}