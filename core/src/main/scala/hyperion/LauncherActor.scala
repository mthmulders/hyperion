package hyperion

import akka.actor.{ActorLogging, Actor, Props}
import akka.io.IO
import akka.io.Tcp.{CommandFailed, Bound}
import spray.can.Http

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
  override def preStart = {
    implicit val system = context.system
    val httpRequestActor = system.actorOf(IncomingHttpActor.props(), "incoming-http-actor")
    val messageDistributor = system.actorOf(MessageDistributor.props(), "receiver")

    IO(Http) ! Http.Bind(httpRequestActor, interface = "0.0.0.0", port = port)
  }

  override def receive: Receive = {
    case Bound(address) =>
      log.info("Succesfully bound to address {}:{}", address.getHostName, address.getPort)
    case c: CommandFailed =>
      log.error("Could not bind to requested address due to {}", c)
      context.system.terminate()
  }
}
