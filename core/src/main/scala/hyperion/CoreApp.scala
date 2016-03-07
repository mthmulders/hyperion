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
object CoreApp extends App {
  val system = ActorSystem("hyperion-system")

  sys.addShutdownHook({
    val logging = Logging(system, getClass.getName)

    logging.info("Shutting down the Hyperion Core")
    val termination: Future[Terminated] = system.terminate()
    termination onComplete {
      case Success(_)     => logging.info("Clean shut down complete")
      case Failure(cause) => logging.info(s"Shut down with problems: ${cause.getMessage}", cause)
    }
    Await.result(termination, Duration.Inf)
  })

  new CoreApp(system).run()
}

class CoreApp(system: ActorSystem) {
  private[this] val log = Logging(system, getClass.getName)

  log.info("Starting the Hyperion Core")

  private val receiver = createReceiver()

  protected def createReceiver(): ActorRef = {
    system.actorOf(MessageDistributor.props(), "receiver")
  }

  def run(): Unit = {
    Await.result(system.whenTerminated, Duration.Inf)
  }
}