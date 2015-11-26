package hyperion

import akka.actor.{Terminated, ActorSystem}
import akka.event.Logging

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future, Await}
import scala.util.{Failure, Success}

/**
  * Starts the Meter Agent.
  */
object MeterAgentApp {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("hyperion-system")
    val logging = Logging(system, getClass.getName)

    logging.info("Started the Hyperion Meter Agent")

    logging.info("Reading settings")
    val settings = Settings(system)

    sys.addShutdownHook({
      import ExecutionContext.Implicits.global

      logging.info("Shutting down the Hyperion Meter Agent")
      val termination: Future[Terminated] = system.terminate()
      termination onComplete {
        case Success(_)     => logging.info("Clean shut down complete")
        case Failure(cause) => logging.info(s"Shut down with problems: ${cause.getMessage}", cause)
      }
      Await.result(termination, Duration.Inf)
    })
  }
}
