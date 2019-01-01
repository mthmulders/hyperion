package hyperion.rest

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.{Directives, Route}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import hyperion.BuildInfo

/**
  * Provides the Spray route to retrieve some system information.
  */
class AppInfoService extends Directives with DefaultJsonProtocol with SprayJsonSupport {
  final case class AppInfo(appVersion: String,
                           scalaVersion: String,
                           javaVersion: String,
                           os: String,
                           totalMem: String,
                           freeMem: String,
                           database: String)
  implicit val appInfoJsonFormat: RootJsonFormat[AppInfo] = jsonFormat7(AppInfo)

  val route: Route = path("info") {
    get {
      complete {
        AppInfo(
          BuildInfo.version,
          BuildInfo.scalaVersion,
          AppInfoHelper.javaVersion,
          AppInfoHelper.os,
          AppInfoHelper.totalMem(),
          AppInfoHelper.freeMem(),
          AppInfoHelper.database)
      }
    }
  }
}
