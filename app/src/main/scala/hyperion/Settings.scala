package hyperion

import akka.actor.{Actor, ExtendedActorSystem, Extension, ExtensionKey}
import com.github.jodersky.flow.{Parity => EParity}
import com.github.jodersky.flow.Parity.Parity
import scala.concurrent.duration._

/**
  * Provides convenient access to the settings in application.conf.
  */
object Settings extends ExtensionKey[Settings]

class Settings(system: ExtendedActorSystem) extends Extension {
  private val hyperion = system.settings.config getConfig "hyperion"

  object api {
    val port               = hyperion getInt    "api.port"
  }

  object history {
    val resolution        = hyperion getDuration("history.resolution", MILLISECONDS) millis
    val limit             = hyperion getDuration("history.limit", HOURS) hours
  }

  object daily {
    val resolution        = hyperion getDuration("daily.resolution", MILLISECONDS) millis
  }

  object database {
    val driver            = hyperion getString "database.driver"
    val user              = hyperion getString "database.user"
    val password          = hyperion getString "database.password"
    val url               = hyperion getString "database.url"
  }

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
