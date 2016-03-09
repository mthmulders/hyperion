package hyperion

import akka.actor.{Actor, ExtendedActorSystem, Extension, ExtensionKey}

/**
  * Provides convenient access to the settings in application.conf.
  */
object Settings extends ExtensionKey[Settings]

class Settings(system: ExtendedActorSystem) extends Extension {
  private val hyperion = system.settings.config getConfig "hyperion"

  object api {
    val port = hyperion getInt "api.port"
  }

}

trait SettingsActor {
  this: Actor =>

  val settings: Settings = Settings(context.system)
}
