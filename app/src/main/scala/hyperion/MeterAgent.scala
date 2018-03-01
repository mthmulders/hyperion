package hyperion

import akka.actor.{Actor, ActorLogging, ActorRef, Terminated}
import akka.io.IO
import akka.serial.{Serial, SerialSettings}

import MeterAgent._

object MeterAgent {
  case class IncomingData(data: String)
}

/**
  * This actor is an intermediary between Flow (for reading the serial port) and the Hyperion Meter Agent. It receives data from Flow and forwards it for processing to the Collecting Actor.
  * @param collectingActor The Collecting Actor that collects data from the serial line.
  */
class MeterAgent(collectingActor: ActorRef) extends Actor with ActorLogging with AppSettings {
  private val operator = IO(Serial)(context.system)

  override def preStart: Unit = {
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

  override def receive: Receive = {
    case Serial.CommandFailed(command, reason) =>
      val commandName = command.getClass.getSimpleName.toLowerCase
      val reasonName = reason.getClass.getSimpleName
      log.error(s"Could not $commandName serial port due to $reasonName")
    case Serial.Opened(openedPort) =>
      log.info(s"Opened serial port $openedPort")
    case Serial.Received(bytes) =>
      collectingActor ! IncomingData(bytes.utf8String)
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
