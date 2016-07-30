package hyperion.rest

import hyperion.BuildInfo
import org.slf4j.LoggerFactory
import spray.httpx.SprayJsonSupport
import spray.json.DefaultJsonProtocol
import spray.routing.Directives

/**
  * Provides the Spray route to retrieve some system information.
  */
class AppInfoService extends Directives with DefaultJsonProtocol with SprayJsonSupport {
  private val logger = LoggerFactory.getLogger(getClass)

  case class AppInfo(appVersion: String,
                     scalaVersion: String,
                     javaVersion: String,
                     os: String,
                     totalMem: String,
                     freeMem: String,
                     database: String)
  implicit val appInfoJsonFormat = jsonFormat7(AppInfo)

  val route = path("info") {
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
