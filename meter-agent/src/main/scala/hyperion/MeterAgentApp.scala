package hyperion

import akka.actor.{Actor, ActorLogging, ActorRef, Terminated, ActorSystem, Props}
import akka.event.Logging

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

/**
  * Starts the Meter Agent.
  */
object MeterAgentApp {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("hyperion-system")

    sys.addShutdownHook({
      val logging = Logging(system, getClass.getName)

      logging.info("Shutting down the Hyperion Meter Agent")
      val termination: Future[Terminated] = system.terminate()
      termination onComplete {
        case Success(_)     => logging.info("Clean shut down complete")
        case Failure(cause) => logging.info(s"Shut down with problems: ${cause.getMessage}", cause)
      }
      Await.result(termination, Duration.Inf)
    })

    new MeterAgentApp(system).run()
  }
}

class MeterAgentApp(system: ActorSystem) {
  private[this] val log = Logging(system, getClass.getName)

  log.info("Reading settings")
  private val settings = Settings(system)

  private val collectingActor = createCollectingActor()

  log.info("Starting the Hyperion Meter Agent")
  private val meterAgent = createMeterAgent()

  def run(): Unit = {
    Await.result(system.whenTerminated, Duration.Inf)
  }

  protected def createMeterAgent(): ActorRef = {
    system.actorOf(MeterAgent.props(collectingActor), "meter-agent")
  }

  protected def createCollectingActor(): ActorRef = {
    val receiver = system.actorOf(Props(new LoggingActor()))
    system.actorOf(CollectingActor.props(receiver), "collecting-actor")
  }

}

class LoggingActor extends Actor with ActorLogging {
  override def receive = {
    case a: Any => log.info(s"Received message $a")
  }
}