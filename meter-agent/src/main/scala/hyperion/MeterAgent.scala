package hyperion

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import akka.io.IO
import com.github.jodersky.flow.{Serial, Parity, SerialSettings}

object MeterAgent {
  def props(): Props = {
    Props(new MeterAgent)
  }
}

class MeterAgent extends Actor with ActorLogging with SettingsActor {
  private val port = settings.meter.serialPort
  private val serialSettings = SerialSettings(
    baud = settings.meter.baudRate,
    characterSize = settings.meter.characterSize,
    twoStopBits = settings.meter.stopBits == 2,
    parity = Parity.withName(settings.meter.parity)
  )
  log.debug(s"Opening serial port $port")
  private val operator = IO(Serial)(context.system)
  operator ! Serial.Open(port, serialSettings)

  override def receive = {
    case Serial.CommandFailed(command, reason) =>
      val commandName = command.getClass.getSimpleName.toLowerCase
      val reasonName = reason.getClass.getSimpleName
      log.error(reason, s"Could not $commandName serial port due to $reasonName")
    case Serial.Opened(openedPort) =>
      val message = s"Opened serial port $openedPort"
      log.info(message)
      context become opened(sender)
    case a: Any =>
      log.debug(s"Ignoring message $a")
  }

  def opened(operator: ActorRef): Receive = {
    // Other messages that can be received:
    // - Received(ByteString(48, 45, 48, 58, 57, 54, 46, 49))
    case Serial.Closed =>
      log.info("Serial port operator closed normally")
    case Terminated(`operator`) =>
      log.error("Serial port operator crashed unexpectedly")
    case a: Any =>
      log.debug(s"Ignoring message $a")
  }

  override def postStop(): Unit = {
    operator ! Serial.Close
  }
}
