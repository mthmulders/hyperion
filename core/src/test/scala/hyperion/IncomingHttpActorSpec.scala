package hyperion

import akka.actor.ActorDSL.actor
import akka.testkit.TestProbe
import org.scalatest.OptionValues
import spray.http.{HttpEntity, HttpRequest, HttpResponse, StatusCodes, Uri}
import spray.http.ContentTypes.`application/json`
import spray.http.HttpMethods.{GET, POST, PUT}
import spray.http.HttpProtocols.`HTTP/1.1`

class IncomingHttpActorSpec extends BaseAkkaSpec with OptionValues {
  "Receiving an incoming HTTP request" should {
    "reject requests to the wrong path" in {
      // Arrange
      val client = TestProbe()
      val request = new HttpRequest(GET, Uri("/wrong"), Nil, HttpEntity.Empty, `HTTP/1.1`)

      // Act
      val sut = actor("wrong-path")(new IncomingHttpActor(TestProbe().ref))
      client.send(sut, request)

      // Assert
      val response = client.expectMsgAllClassOf(classOf[HttpResponse]).headOption
      response.value.status should be(StatusCodes.NotFound)
      response.value.entity.asString should include("Not found")
    }

    "reject requests with the wrong HTTP method" in {
      // Arrange
      val client = TestProbe()
      val request = new HttpRequest(PUT, Uri("/"), Nil, HttpEntity.Empty, `HTTP/1.1`)

      // Act
      val sut = actor("wrong-method")(new IncomingHttpActor(TestProbe().ref))
      client.send(sut, request)

      // Assert
      val response = client.expectMsgAllClassOf(classOf[HttpResponse]).headOption
      response.value.status should be(StatusCodes.NotFound)
      response.value.entity.asString should include("Not found")
    }
  }
}
