package hyperion

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import akka.actor.{ActorRefFactory, ActorSystem, Props, Terminated}
import akka.event.Logging
import akka.io.IO
import hyperion.database.MeterReadingDAO
import hyperion.rest.RestApi
import hyperion.ws.WebSocketApi
import spray.can.Http
import spray.can.server.UHttp

/**
  * Core is type containing the ``system: ActorSystem`` member. This enables us to use it in our
  * apps as well as in our tests.
  */
trait Core {
  protected implicit def system: ActorSystem
  protected implicit def settings = AppSettings(system)
}

/**
  * This trait implements ``Core`` by starting the required ``ActorSystem`` and registering the
  * termination handler to stop the system when the JVM exits.
  */
trait BootedCore extends Core with RestApi with WebSocketApi {
  private[this] val log = Logging(system, getClass.getName)

  implicit lazy val system = ActorSystem("hyperion-system")
  def actorRefFactory: ActorRefFactory = system

  val rootService = system.actorOf(Props(new RoutedHttpService(restRoutes ~ webSocketRoutes)))

  IO(UHttp)(system) ! Http.Bind(rootService, "0.0.0.0", settings.api.port)

  sys.addShutdownHook({
    log.info("Shutting down the Hyperion Core")
    IO(UHttp)(system) ! Http.Unbind
    val termination: Future[Terminated] = system.terminate()
    termination onComplete {
      case Success(_)     => log.info("Clean shut down complete")
      case Failure(cause) => log.info(s"Shut down with problems: ${cause.getMessage}", cause)
    }
    Await.result(termination, Duration.Inf)
  })
}

/**
  * This trait contains the actors that make up the application; it can be mixed in with
  * ``BootedCore`` for running code or ``TestKit`` for unit and integration tests.
  */
trait HyperionActors { this: Core =>
  val messageDistributor = system.actorOf(Props(new MessageDistributor()), "receiver")
  val collectingActor = system.actorOf(Props(new CollectingActor(messageDistributor)), "collecting-actor")
  val meterAgent = system.actorOf(Props(new MeterAgent(messageDistributor, settings)), "meter-agent")
  val recentHistoryActor = system.actorOf(Props(new RecentHistoryActor(messageDistributor, settings)), "recent-history")
  val dailyHistoryActor = system.actorOf(Props(new DailyHistoryActor(messageDistributor, new MeterReadingDAO(), settings)), "daily-history")
}
