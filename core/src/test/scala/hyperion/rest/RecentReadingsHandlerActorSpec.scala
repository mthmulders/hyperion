package hyperion.rest

import akka.actor.ActorDSL.actor
import akka.testkit.TestProbe
import hyperion.{BaseAkkaSpec, TestSupport}
import hyperion.RecentHistoryActor.{GetRecentHistory, RecentReadings}
import spray.http.HttpMethods._
import spray.http.HttpProtocols._
import spray.http.{HttpEntity, HttpRequest, HttpResponse, StatusCodes, Uri}
import scala.collection.immutable

class RecentReadingsHandlerActorSpec extends BaseAkkaSpec {
  "The RecentReadingsRequestHandling Actor" should {
    "Supply the recent meter readings in JSON" in {
      // Arrange
      val client = TestProbe()
      val recentHistoryActor = TestProbe()
      val sut = actor("recent-readings")(new RecentReadingsHandlerActor(recentHistoryActor.ref))
      val telegram = TestSupport.randomTelegram()
      val history = immutable.Vector(telegram)

      // Act
      client.send(sut, HttpRequest(GET, Uri("/recent"), Nil, HttpEntity.Empty, `HTTP/1.1`))
      recentHistoryActor.expectMsg(GetRecentHistory)
      recentHistoryActor.reply(RecentReadings(history))

      // Assert
      val response = client.expectMsgAnyClassOf(classOf[HttpResponse])
      response.status should be(StatusCodes.OK)
      response.entity should not be HttpEntity.Empty
      response.entity.toString should include(s""""tariff":"${telegram.data.currentTariff}"""")
    }
  }
}
