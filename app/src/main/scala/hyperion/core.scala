package hyperion

import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.{ActorSystem, DeadLetter, Props}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.pattern.{BackoffOpts, BackoffSupervisor}
import akka.stream.{ActorMaterializer, Materializer}
import hyperion.database.DatabaseActor
import hyperion.rest.HttpApi

/**
  * Core is type containing the ``system: ActorSystem`` member. This enables us to use it in our
  * apps as well as in our tests.
  */
trait Core {
  protected implicit def system: ActorSystem
  protected implicit def settings: AppSettingsImpl = AppSettings(system)
}

/**
  * This trait implements ``Core`` by starting the required ``ActorSystem`` and registering the
  * termination handler to stop the system when the JVM exits.
  */
trait BootedCore extends Core with HttpApi {
  override protected implicit def system: ActorSystem = ActorSystem("hyperion")
  private[this] implicit val materializer: Materializer = ActorMaterializer()
  private[this] val log = Logging(system, getClass.getName)

  log.info(s"Starting Hyperion at port ${settings.api.port}")

  private[this] val bindingFuture = Http().newServerAt("0.0.0.0", settings.api.port).bindFlow(routes)

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
  val deadLetterLogger = system.actorOf(DeadLetterLoggingActor.props(), "dead-letter-logging")
  system.eventStream.subscribe(deadLetterLogger, classOf[DeadLetter])

  val messageDistributor = system.actorOf(Props(new MessageDistributor()), "receiver")
  val collectingActor = system.actorOf(Props(new CollectingActor(messageDistributor)), "collecting-actor")
  val meterAgent = system.actorOf(Props(new MeterAgent(collectingActor)), "meter-agent")
  val recentHistoryActor = system.actorOf(Props(new RecentHistoryActor(messageDistributor)), "recent-history")

  val supervisor = BackoffSupervisor.props(
    BackoffOpts.onStop(
      childProps = Props(new DatabaseActor()),
      childName = "database",
      minBackoff = 5 seconds,
      maxBackoff = 60 seconds,
      randomFactor = 0.2
    )
  )

  val databaseActor = system.actorOf(supervisor, "database-supervisor")
  val dailyHistoryActor = system.actorOf(Props(new DailyHistoryActor(messageDistributor, databaseActor)), "daily-history")
  val usageCalculationActor = system.actorOf(Props(new UsageCalculationActor(databaseActor)), "usage-calculation")
}
