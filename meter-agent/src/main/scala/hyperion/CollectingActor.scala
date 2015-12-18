package hyperion

import akka.actor.{ActorRef, ActorLogging, Actor, Props}
import hyperion.CollectingActor.TelegramReceived

import scala.collection.mutable
import scala.collection.immutable

object CollectingActor {
  case class TelegramReceived(lines: immutable.Seq[String])
  def props(receiver: ActorRef): Props = {
    Props(new CollectingActor(receiver))
  }
}

/** This actor collects incoming bytes and emits them in lines (seperated by CR LF) */
class CollectingActor(receiver: ActorRef) extends Actor with ActorLogging {
  private val CRLF = "\r\n"

  override def receive = skippingFirstTelegram(new StringBuilder)

  /**
    * In this (initial) state, we skip data until we found a new Telegram.
    * @param dataBuffer Buffer of data that has already been received, but is not processed.
    */
  def skippingFirstTelegram(dataBuffer: StringBuilder): Receive = {
    case MeterAgent.IncomingData(data) =>
      dataBuffer.append(data)
      var linebreak = dataBuffer.indexOf(CRLF)
      while (linebreak != -1) {
        dataBuffer.replace(0, linebreak + CRLF.length, "")
        linebreak = dataBuffer.indexOf(CRLF)
      }
      if (dataBuffer.startsWith("/")) {
        context become bufferingTelegram(dataBuffer, mutable.Buffer.empty[String])
      } else {
        context become skippingFirstTelegram(dataBuffer)
      }
  }

  /**
    * In this (normal) state, we buffer data until we found a new line.
    * @param dataBuffer Buffer of data that has already been received, but is not processed.
    * @param lineBuffer Buffer of lines that have already been received, but are not processed
    */
  def bufferingTelegram(dataBuffer: StringBuilder, lineBuffer: mutable.Buffer[String]): Receive = {
    case MeterAgent.IncomingData(data) =>
      dataBuffer.append(data)
      var linebreak = dataBuffer.indexOf(CRLF)
      while (linebreak != -1) {
        val line = dataBuffer.substring(0, linebreak + CRLF.length)
        lineBuffer += line.replace("\n", "").replace("\r", "")
        dataBuffer.replace(0, linebreak + CRLF.length, "")

        if (lineBuffer.last.startsWith("!")) {
          // The line buffer now contains a complete Telegram. Emit it to our receiver and clear the buffer.
          receiver ! TelegramReceived(lineBuffer.toIndexedSeq)
          lineBuffer.clear()
          linebreak = -1
        } else {
          linebreak = dataBuffer.indexOf(CRLF)
        }
      }
      context become bufferingTelegram(dataBuffer, lineBuffer)
  }
}
