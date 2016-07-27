package hyperion.ws

import scala.concurrent.duration.DurationInt
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import hyperion.Core
import org.slf4j.LoggerFactory
import spray.routing.Directives

import scala.concurrent.ExecutionContext

/**
  * Defines the WebSocket API for getting live updates from the metering system.
  * @param messageDistributor Ref to the Actor that distributes messages.
  * @param system Actor system
  * @param executionContext an ``ExecutionContext`` for handling ``Future``s.
  */
class ActualReadingsService(val messageDistributor: ActorRef, val system: ActorSystem)(implicit executionContext: ExecutionContext)
  extends Directives with Core {

  private[this] val log = LoggerFactory.getLogger(getClass)

  implicit val timeout = Timeout(2.seconds)

  val route = path("actual") {
    get { ctx =>
      val client = ctx.responder
      val worker = system.actorOf(Props(new ActualReadingsClientWorker(messageDistributor, client)))
      (worker ? ctx.request).map(res => client ! res)
    }
  }

}