package hyperion

import akka.actor.{ActorLogging, Actor, Props}
import akka.io.IO
import akka.io.Tcp.{CommandFailed, Bound}
import spray.can.Http
import spray.can.server.UHttp

/** Companion object for the [[LauncherActor]] class */
object LauncherActor {
  def props(): Props = {
    Props(new LauncherActor())
  }
}

/**
  * Actor that tries to allocate the configured TCP-port for listening. If this fails, close the Actor system.
  */
class LauncherActor() extends Actor with ActorLogging with SettingsActor {
  protected def http() = {
    IO(UHttp)(context.system)
  }

  override def preStart = {
    val system = context.system

    val messageDistributor = system.actorOf(MessageDistributor.props(), "receiver")

    val collectingActor = system.actorOf(CollectingActor.props(messageDistributor), "collecting-actor")

    system.actorOf(MeterAgent.props(collectingActor), "meter-agent")

    system.actorOf(RecentHistoryActor.props(messageDistributor), "recent-history")

    val httpRequestActor = context.system.actorOf(IncomingHttpActor.props(messageDistributor), "incoming-http-actor")

    http ! Http.Bind(httpRequestActor, interface = "0.0.0.0", port = settings.api.port)
  }

  override def receive: Receive = {
    case Bound(address) =>
      log.info("Succesfully bound to address {}:{}", address.getHostName, address.getPort)
    case c: CommandFailed =>
      log.error("Could not bind to requested address due to {}", c)
      context.system.terminate()
  }

  override def postStop = {
    http ! Http.Unbind
  }
}
