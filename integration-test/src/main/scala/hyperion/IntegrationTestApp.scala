package hyperion

//import akka.actor.ActorSystem

import akka.actor.ActorSystem

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory

object IntegrationTestApp {
  def apply(port: Int): IntegrationTestApp = {
    val custom = ConfigFactory.parseString(s"""
       akka {
         loggers: [akka.event.slf4j.Slf4jLogger]
       }
       hyperion {
         api {
             port: $port
         }
       }
    """)
    val default = ConfigFactory.load(getClass.getClassLoader)
    val config = custom.withFallback(default)
    new IntegrationTestApp(config)
  }
}

/**
  * Special CLI for running Hyperion integration tests without a smart meter.
  */
class IntegrationTestApp(config: Config) extends BootedCore with HyperionIntegrationTestActors {
  override protected implicit def system: ActorSystem = ActorSystem("hyperion", config)

  def stop(): Unit = {
    Await.result(system.terminate(), 5 seconds)
  }
}