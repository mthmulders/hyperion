package hyperion

import akka.actor.{Address, ActorSystem, ActorRef, RootActorPath, Terminated}
import akka.event.Logging

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

/**
  * Starts the Meter Agent.
  */
object MeterAgentApp extends App {
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

class MeterAgentApp(system: ActorSystem) {
  private[this] val log = Logging(system, getClass.getName)

  log.info("Reading settings")
  private val settings = Settings(system)

  log.info("Starting the Hyperion Meter Agent")
  private val telegramReceiver = findTelegramReceiver()
  private val collectingActor = createCollectingActor()
  private val meterAgent = createMeterAgent()

  def run(): Unit = {
    Await.result(system.whenTerminated, Duration.Inf)
  }

  protected def findTelegramReceiver(): ActorRef = {
    val path = RootActorPath(Address("akka.tcp", system.name, settings.receiver.host, 2552)) / "user" / "receiver"
    system.actorOf(ForwardingActor.props(system.actorSelection(path)), "forwarding-actor")
  }

  protected def createMeterAgent(): ActorRef = {
    system.actorOf(MeterAgent.props(collectingActor), "meter-agent")
  }

  protected def createCollectingActor(): ActorRef = {
    system.actorOf(CollectingActor.props(telegramReceiver), "collecting-actor")
  }
}
