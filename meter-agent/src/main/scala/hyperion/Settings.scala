package hyperion

import akka.actor.{Actor, Extension, ExtendedActorSystem, ExtensionKey}

/**
  * Provides convenient access to the settings in application.conf.
  */
object Settings extends ExtensionKey[Settings]

class Settings(system: ExtendedActorSystem) extends Extension {
  private val hyperion = system.settings.config getConfig "hyperion"

  object meter {
    val serialPort: String = hyperion getString "meter.serial-port"
    val baudRate: Int      = hyperion getInt    "meter.baud-rate"
    val characterSize: Int = hyperion getInt    "meter.character-size"
    val stopBits: Int      = hyperion getInt    "meter.stop-bits"
    val parity: String     = hyperion getString "meter.parity"
  }
}

trait SettingsActor {
  this: Actor =>

  val settings: Settings = Settings(context.system)
}
