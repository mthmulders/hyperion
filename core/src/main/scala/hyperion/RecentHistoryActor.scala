package hyperion

import akka.actor.{ActorLogging, ActorRef, FSM, Props}
import hyperion.MessageDistributor.RegisterReceiver
import hyperion.RecentHistoryActor._

object RecentHistoryActor {
  def props(messageDistributor: ActorRef) = {
    Props(new RecentHistoryActor(messageDistributor))
  }

  sealed trait State
  case object Sleeping extends State
  case object Receiving extends State
  sealed trait Data
  case class History(telegrams: RingBuffer[P1Telegram]) extends Data

  case object GetRecentHistory
  case class RecentReadings()
}

/**
  * Actor that returns the most recent meter readings.
  *
  * @param messageDistributor The Actor that distributes incoming telegrams.
  */
class RecentHistoryActor(messageDistributor: ActorRef) extends FSM[State, Data] with ActorLogging with SettingsActor {
  override def preStart = {
    messageDistributor ! RegisterReceiver
  }

  private[this] val historyLimit = (settings.history.limit / settings.history.resolution).toInt
  log.debug(s"Allocating buffer for $historyLimit entries")
  startWith(Receiving, History(RingBuffer[P1Telegram](historyLimit)))

  when(Receiving) {
    case Event(TelegramReceived(telegram), History(history)) =>
      log.info(s"Received telegram issued @ ${telegram.metadata.timestamp}")
      log.debug(s"Will now sleep for ${settings.history.resolution}")
      goto(Sleeping) using History(history += telegram)
  }

  when(Sleeping) {
    case Event(_: TelegramReceived, _) =>
      log.debug("Not storing this telegram")
      stay()
  }

  when(Sleeping, settings.history.resolution) {
    case Event(StateTimeout, history) =>
      log.debug("Waiting for next telegram to come in")
      goto(Receiving) using history
  }

  initialize()
}
