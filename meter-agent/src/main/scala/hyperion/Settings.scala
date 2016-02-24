package hyperion

import akka.actor.{Actor, Extension, ExtendedActorSystem, ExtensionKey}
import com.github.jodersky.flow.{Parity => EParity}
import com.github.jodersky.flow.Parity.Parity

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
    val parity: Parity     = EParity.values.find(_.toString.equalsIgnoreCase(hyperion getString "meter.parity"))
                                  .getOrElse(EParity.None)
  }
}

trait SettingsActor {
  this: Actor =>

  val settings: Settings = Settings(context.system)
}
