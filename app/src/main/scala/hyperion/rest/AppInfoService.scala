package hyperion.rest

import scala.concurrent.duration.DurationInt

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.{Directives, Route}
import akka.pattern.ask
import akka.util.Timeout
import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import hyperion.BuildInfo
import hyperion.database.DatabaseActor.GetDatabaseInfo

/**
  * Provides the Spray route to retrieve some system information.
  */
class AppInfoService(databaseActor: ActorRef) extends Directives with DefaultJsonProtocol with SprayJsonSupport {
  final case class AppInfo(appVersion: String,
                           scalaVersion: String,
                           javaVersion: String,
                           os: String,
                           totalMem: String,
                           freeMem: String,
                           database: String)
  implicit val appInfoJsonFormat: RootJsonFormat[AppInfo] = jsonFormat7(AppInfo)
  implicit val timeout: Timeout = Timeout(5 seconds)

  val route: Route = path("info") {
    get {
      val query = (databaseActor ? GetDatabaseInfo).mapTo[String]
      onSuccess(query) { database =>
        complete(
          AppInfo(
            BuildInfo.version,
            BuildInfo.scalaVersion,
            AppInfoHelper.javaVersion,
            AppInfoHelper.os,
            AppInfoHelper.totalMem(),
            AppInfoHelper.freeMem(),
            database)
        )
      }
    }
  }
}
