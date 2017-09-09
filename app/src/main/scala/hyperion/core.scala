package hyperion

import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.{ActorSystem, Props}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.{ActorMaterializer, Materializer}

import hyperion.database.MeterReadingDAO
import hyperion.rest.HttpApi

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
trait BootedCore extends Core with HttpApi {
  override protected implicit def system = ActorSystem("hyperion")
  private[this] implicit val materializer: Materializer = ActorMaterializer()
  private[this] val log = Logging(system, getClass.getName)

  log.info("Starting Hyperion")

  private[this] val bindingFuture = Http().bindAndHandle(Route.handlerFlow(routes), "0.0.0.0", settings.api.port)

  sys.addShutdownHook({
    log.info("Shutting down Hyperion")
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  })
}

/**
  * This trait contains the actors that make up the application; it can be mixed in with
  * ``BootedCore`` for running code or ``TestKit`` for unit and integration tests.
  */
trait HyperionActors { this: Core =>
  val messageDistributor = system.actorOf(Props(new MessageDistributor()), "receiver")
  val collectingActor = system.actorOf(Props(new CollectingActor(messageDistributor)), "collecting-actor")
  val meterAgent = system.actorOf(Props(new MeterAgent(collectingActor)), "meter-agent")
  val recentHistoryActor = system.actorOf(Props(new RecentHistoryActor(messageDistributor)), "recent-history")
  val dailyHistoryActor = system.actorOf(Props(new DailyHistoryActor(messageDistributor, new MeterReadingDAO())), "daily-history")
}
