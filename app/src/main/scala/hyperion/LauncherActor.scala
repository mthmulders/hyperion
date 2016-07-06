package hyperion

import akka.actor.{ActorLogging, Actor, Props}
import akka.io.IO
import akka.io.Tcp.{CommandFailed, Bound}
import spray.can.Http
import spray.can.server.UHttp

/** Companion object voor the [[LauncherActor]] class */
object LauncherActor {
  def props(): Props = {
    Props(new LauncherActor())
  }
}

/**
  * Actor that tries to allocate the configured TCP-poort for listening. If this fails, close the Actor system.
  *
  * @param port Desired port number.
  */
class LauncherActor() extends Actor with ActorLogging with SettingsActor {
  protected def http() = {
    implicit val system = context.system
    IO(UHttp)
  }

  override def preStart = {
    val system = context.system;
    val messageDistributor = system.actorOf(MessageDistributor.props(), "receiver")

    val collectingActor = system.actorOf(CollectingActor.props(messageDistributor), "collecting-actor")

    val meterAgent = system.actorOf(MeterAgent.props(collectingActor), "meter-agent")

    context.system.actorOf(RecentHistoryActor.props(messageDistributor), "recent-history")

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
