package hyperion

import scala.collection.immutable

import akka.actor.{ActorLogging, ActorRef, FSM}

import hyperion.MessageDistributor.RegisterReceiver
import hyperion.RecentHistoryActor._
import hyperion.p1._

object RecentHistoryActor {
  sealed trait State
  case object Sleeping extends State
  case object Receiving extends State
  sealed trait Data
  final case class History(telegrams: RingBuffer[P1Telegram]) extends Data

  case object GetRecentHistory
  final case class RecentReadings(telegrams: immutable.Vector[P1Telegram])
}

/**
  * Actor that keeps the most recent meter readings in memory.
  *
  * @param messageDistributor The Actor that distributes incoming telegrams.
  */
class RecentHistoryActor(messageDistributor: ActorRef)
  extends FSM[RecentHistoryActor.State, RecentHistoryActor.Data]
  with ActorLogging with AppSettings {

  
  override def preStart: Unit = {
    messageDistributor ! RegisterReceiver
  }

  private[this] val historyLimit = (settings.history.limit / settings.history.resolution).toInt
  log.info(s"Keeping readings every ${settings.history.resolution} for ${settings.history.limit} - allocating buffer for $historyLimit entries")

  startWith(Receiving, History(RingBuffer[P1Telegram](historyLimit)))

  when(Receiving) {
    case Event(TelegramReceived(telegram), History(history)) =>
      log.debug("Sleeping for {}", settings.history.resolution)
      goto(Sleeping) using History(history += telegram)
    case Event(GetRecentHistory, History(history)) =>
      sender() ! RecentReadings(history.toVector)
      stay()
    case Event(StateTimeout, _) =>
      // Ignored
      stay()
  }

  when(Sleeping) {
    case Event(_: TelegramReceived, _) =>
      stay()
    case Event(GetRecentHistory, History(history)) =>
      sender() ! RecentReadings(history.toVector)
      stay()
    case Event(StateTimeout, history) =>
      log.debug("Awaking to receive new meter reading")
      goto(Receiving) using history
  }

  setTimer("recent-awake", StateTimeout, settings.history.resolution, repeat = true)

  initialize()
}
