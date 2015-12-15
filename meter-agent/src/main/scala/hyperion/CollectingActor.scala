package hyperion

import akka.actor.{ActorRef, ActorLogging, Actor, Props}
import hyperion.CollectingActor.TelegramReceived

import scala.collection.mutable

object CollectingActor {
  case class TelegramReceived(lines: Seq[String])
  def props(receiver: ActorRef): Props = {
    Props(new CollectingActor(receiver))
  }
}

/** This actor collects incoming bytes and emits them in lines (seperated by CR LF) */
class CollectingActor(receiver: ActorRef) extends Actor with ActorLogging {
  private val CRLF = "\r\n"

  private val dataBuffer = new StringBuilder
  private val lineBuffer = mutable.Buffer.empty[String]

  override def receive = ???
  context become skippingFirstTelegram

  /** In this (initial) state, we skip data until we found a new Telegram. */
  def skippingFirstTelegram: Receive = {
    case MeterAgent.IncomingData(data) =>
      dataBuffer.append(data)
      var linebreak = dataBuffer.indexOf(CRLF)
      while (linebreak != -1) {
        dataBuffer.replace(0, linebreak + CRLF.length, "")
        if (dataBuffer.startsWith("/")) {
          context become bufferingTelegram
        } else {
          linebreak = dataBuffer.indexOf(CRLF)
        }
      }
  }

  /** In this (normal) state, we buffer data until we found a new line. */
  def bufferingTelegram: Receive = {
    case MeterAgent.IncomingData(data) =>
      dataBuffer.append(data)
      var linebreak = dataBuffer.indexOf(CRLF)
      while (linebreak != -1) {
        val line = dataBuffer.substring(0, linebreak + CRLF.length)
        lineBuffer += line
        dataBuffer.replace(0, linebreak + CRLF.length, "")

        if (lineBuffer.last.startsWith("!")) {
          // The lineBuffer now contains a complete Telegram. Emit it.
          val lines = lineBuffer.toSeq
          receiver ! TelegramReceived(lines)
          lineBuffer.clear()
          linebreak = -1
        } else {
          linebreak = dataBuffer.indexOf(CRLF)
        }
      }
  }
}
