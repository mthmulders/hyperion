package hyperion

import akka.actor.{Actor, ExtendedActorSystem, Extension, ExtensionKey}
import scala.concurrent.duration._

/**
  * Provides convenient access to the settings in application.conf.
  */
object Settings extends ExtensionKey[Settings]

class Settings(system: ExtendedActorSystem) extends Extension {
  private val hyperion = system.settings.config getConfig "hyperion"

  object api {
    val port = hyperion getInt "api.port"
  }

  object history {
    val resolution = hyperion getDuration("history.resolution", MILLISECONDS) millis
    val limit = hyperion getDuration("history.limit", HOURS) hours
  }

}

trait SettingsActor {
  this: Actor =>

  val settings: Settings = Settings(context.system)
}
