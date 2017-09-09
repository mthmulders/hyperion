package hyperion

import scala.concurrent.duration._

import akka.actor.{Actor, ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}
import com.github.jodersky.flow.{Parity => EParity}
import com.github.jodersky.flow.Parity.Parity
import com.typesafe.config.Config

/**
  * Provides convenient access to the settings in application.conf.
  */
object AppSettings extends ExtensionId[AppSettingsImpl] with ExtensionIdProvider {
  override def lookup: AppSettings.type = AppSettings
  override def createExtension(system: ExtendedActorSystem) = new AppSettingsImpl(system.settings.config)
}

class AppSettingsImpl(config: Config) extends Extension {
  private val hyperion = config getConfig "hyperion"

  object api {
    val port              = hyperion getInt    "api.port"
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

trait SystemSettings { system: ActorSystem =>
  val settings = AppSettings(system)
}

trait AppSettings { actor: Actor =>
  val settings = AppSettings(context.system)
}