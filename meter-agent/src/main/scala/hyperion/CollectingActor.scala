package hyperion

import akka.actor.{ActorRef, ActorLogging, Actor, Props}

import scala.collection.mutable

import CollectingActor._

object CollectingActor {
  case object ProcessBuffer

  case class TelegramReceived(telegram: P1Telegram)

  def props(receiver: ActorRef): Props = {
    Props(new CollectingActor(receiver))
  }
}

/** This actor collects incoming bytes and emits them in lines (seperated by CR LF) */
class CollectingActor(receiver: ActorRef) extends Actor with ActorLogging {
  private val CRLF = "\r\n"

  private val dataBuffer = new StringBuilder
  private var lineBuffer = mutable.Buffer.empty[String]

  override def receive = {
    case MeterAgent.IncomingData(data) =>
      dataBuffer.append(data)
      extractLines()
      extractTelegram()
  }

  private def extractLines(): Unit = {
    dataBuffer.indexOf(CRLF) match {
      case -1              => ;
      case idx if idx >= 0 =>
        lineBuffer += dataBuffer.substring(0, idx + CRLF.length)
        dataBuffer.replace(0, idx + CRLF.length, "")
        extractLines()
      }
  }

  private def extractTelegram(): Unit = {
    if (lineBuffer.isEmpty) {
      return
    }

    val firstLine = lineBuffer.find(s => s != null && s.length > 0 && s.charAt(0) == '/')
    val lastLine = lineBuffer.find(s => s != null && s.length > 0 && s.charAt(0) == '!')

    (firstLine, lastLine) match {
      case (None, None)              => ;
      case (None, Some(last))        => lineBuffer --= lineBuffer.slice(0, lineBuffer.indexOf(last) + 1)
      case (Some(first), None)       => ;
      case (Some(first), Some(last)) =>
        val telegramLines = lineBuffer.slice(0, lineBuffer.indexOf(lastLine.get) + 1)
        lineBuffer --= telegramLines

        val telegramText = telegramLines.mkString("")

        P1TelegramParser.parse(telegramText) match {
          case Some(telegram) => receiver ! TelegramReceived(telegram)
          case None           => log.info("Failed to parse telegram")
        }
    }
  }
}