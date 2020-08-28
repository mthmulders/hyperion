package hyperion

import scala.concurrent.duration._

import akka.actor.{Actor, ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}
import akka.serial.Parity
import akka.serial.Parity.Parity
import com.typesafe.config.Config

/**
  * Provides convenient access to the settings in application.conf.
  */
object AppSettings extends ExtensionId[AppSettingsImpl] with ExtensionIdProvider {
  override def lookup: AppSettings.type = AppSettings
  override def createExtension(system: ExtendedActorSystem) = new AppSettingsImpl(system.settings.config)
}

class AppSettingsImpl(config: Config) extends Extension {
  protected val hyperion = config getConfig "hyperion"

  object api {
    val port              = hyperion getInt    "api.port"
  }

  object history {
    val resolution        = hyperion getDuration("history.resolution", MILLISECONDS) millis
    val limit             = hyperion getDuration("history.limit", MINUTES) minutes
  }

  object meter {
    private val __parity   = hyperion getString "meter.parity"

    val serialPort: String = hyperion getString "meter.serial-port"
    val baudRate: Int      = hyperion getInt    "meter.baud-rate"
    val characterSize: Int = hyperion getInt    "meter.character-size"
    val stopBits: Int      = hyperion getInt    "meter.stop-bits"
    val parity: Parity     = Parity.values.find(_.toString equalsIgnoreCase __parity).getOrElse(Parity.None)
  }
}

trait SystemSettings { system: ActorSystem =>
  val settings = AppSettings(system)
}

trait AppSettings { actor: Actor =>
  val settings = AppSettings(context.system)
}