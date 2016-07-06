package hyperion

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import akka.io.IO
import com.github.jodersky.flow.{Serial, SerialSettings}

import MeterAgent._


object MeterAgent {
  case class IncomingData(data: String)

  def props(collectingActor: ActorRef): Props = {
    Props(new MeterAgent(collectingActor))
  }
}

/**
  * This actor is an intermediary between Flow (for reading the serial port) and the Hyperion Meter Agent. It receives data from Flow and forwards it for processing to the Collecting Actor.
  * @param collectingActor The Collecting Actor that collects data from the serial line.
  */
class MeterAgent(collectingActor: ActorRef) extends Actor with ActorLogging with SettingsActor {
  private val operator = IO(Serial)(context.system)

  override def preStart = {
    val port = settings.meter.serialPort
    val serialSettings = SerialSettings(
      baud = settings.meter.baudRate,
      characterSize = settings.meter.characterSize,
      twoStopBits = settings.meter.stopBits == 2,
      parity = settings.meter.parity
    )

    log.debug(s"Opening serial port $port")
    operator ! Serial.Open(port, serialSettings)
  }

  override def receive = {
    case Serial.CommandFailed(command, reason) =>
      val commandName = command.getClass.getSimpleName.toLowerCase
      val reasonName = reason.getClass.getSimpleName
      log.error(reason, s"Could not $commandName serial port due to $reasonName")
    case Serial.Opened(openedPort) =>
      val message = s"Opened serial port $openedPort"
      log.info(message)
    case Serial.Received(bytes) =>
      val data = bytes.utf8String
      collectingActor ! IncomingData(data)
    case Serial.Closed =>
      log.info("Serial port operator closed normally")
    case Terminated(_) =>
      log.error("Serial port operator crashed unexpectedly")
    case a: Any =>
      log.debug(s"Ignoring message $a")
  }

  override def postStop(): Unit = {
    operator ! Serial.Close
  }
}
