package hyperion

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import scala.collection.mutable
import scala.concurrent.Future
import scala.util.{Failure, Success}

import hyperion.p1.{P1TelegramParser, TelegramReceived}

object CollectingActor {
  case object ProcessBuffer

  def props(receiver: ActorRef): Props = {
    Props(new CollectingActor(receiver))
  }
}

/** This actor collects incoming bytes and emits them in lines (separated by CR LF) */
class CollectingActor(receiver: ActorRef) extends Actor with ActorLogging {
  private val newline = "\r\n"

  private val dataBuffer = new StringBuilder
  private val lineBuffer = mutable.Buffer.empty[String]

  override def receive: Receive = {
    case MeterAgent.IncomingData(data) =>
      dataBuffer.append(data)
      extractLines()
      if (lineBuffer.nonEmpty) extractTelegram()
  }

  private def extractLines(): Unit = {
    dataBuffer.indexOf(newline) match {
      case -1              => ;
      case idx if idx >= 0 =>
        lineBuffer += dataBuffer.substring(0, idx + newline.length)
        dataBuffer.replace(0, idx + newline.length, "")
        extractLines()
      }
  }

  private def extractTelegram(): Unit = {
    val firstLine = lineBuffer.find(s => s.length > 0 && s.charAt(0) == '/')
    val lastLine = lineBuffer.find(s => s.length > 0 && s.charAt(0) == '!')

    (firstLine, lastLine) match {
      case (None, None)       => ;
      case (None, Some(last)) => lineBuffer --= lineBuffer.slice(0, lineBuffer.indexOf(last) + 1)
      case (Some(_), None)    => ;
      case (Some(_), Some(last)) =>
        val telegramLines = lineBuffer.slice(0, lineBuffer.indexOf(last) + 1)
        lineBuffer --= telegramLines
        processBuffer(telegramLines)
    }
  }

  private def processBuffer(lines: mutable.Buffer[String]): Unit = {
    import context.dispatcher

    Future {
      val source = lines.mkString("")
      P1TelegramParser.parseTelegram(source) match {
        case Success(telegram) => receiver ! TelegramReceived(telegram)
        case Failure(reason)   => log.error(s"Could not parse telegram: ${reason.getMessage}. Source: \n$source")
      }
    }
  }
}
