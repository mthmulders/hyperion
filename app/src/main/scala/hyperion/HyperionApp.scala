package hyperion

import akka.actor.{ActorRef, ActorSystem, Terminated}
import akka.event.Logging

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

/**
  * Starts the Hyperion application.
  */
object HyperionApp extends App {
  val system = ActorSystem("hyperion-system")

  new HyperionApp(system)
  Await.result(system.whenTerminated, Duration.Inf)
}

class HyperionApp(system: ActorSystem) {
  private[this] val log = Logging(system, getClass.getName)

  sys.addShutdownHook({
    log.info("Shutting down the Hyperion Core")
    val termination: Future[Terminated] = system.terminate()
    termination onComplete {
      case Success(_)     => log.info("Clean shut down complete")
      case Failure(cause) => log.info(s"Shut down with problems: ${cause.getMessage}", cause)
    }
    Await.result(termination, Duration.Inf)
  })

  log.info("Reading settings")
  Settings(system)

  log.info("Starting Hyperion")
  system.actorOf(LauncherActor.props(), "launcher-actor")
}
