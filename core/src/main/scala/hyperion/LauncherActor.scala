package hyperion

import akka.actor.{ActorLogging, Actor, Props}
import akka.io.IO
import akka.io.Tcp.{CommandFailed, Bound}
import spray.can.Http
import spray.can.server.UHttp

/** Companion object voor the [[LauncherActor]] class */
object LauncherActor {
  def props(port: Int): Props = {
    Props(new LauncherActor(port))
  }
}

/**
  * Actor that tries to allocate the configured TCP-poort for listening. If this fails, close the Actor system.
  *
  * @param port Desired port number.
  */
class LauncherActor(port: Int) extends Actor with ActorLogging {
  protected def http() = {
    implicit val system = context.system
    IO(UHttp)
  }

  override def preStart = {
    val messageDistributor = context.system.actorOf(MessageDistributor.props(), "receiver")
    context.system.actorOf(RecentHistoryActor.props(messageDistributor), "recent-history")
    val httpRequestActor = context.system.actorOf(IncomingHttpActor.props(messageDistributor), "incoming-http-actor")

    http ! Http.Bind(httpRequestActor, interface = "0.0.0.0", port = port)
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