package hyperion

import akka.actor.{ActorLogging, ActorRef, FSM, Props}
import hyperion.MessageDistributor.RegisterReceiver
import hyperion.DailyHistoryActor._

object DailyHistoryActor {
  def props(messageDistributor: ActorRef) = {
    Props(new DailyHistoryActor(messageDistributor))
  }

  sealed trait State
  case object Sleeping extends State
  case object Receiving extends State

  sealed trait Data
  case object Empty extends Data
}

/**
  * Actor that stores a daily meter reading in an external database
  *
  * @param messageDistributor The Actor that distributes incoming telegrams.
  */
class DailyHistoryActor(messageDistributor: ActorRef)
    extends FSM[DailyHistoryActor.State, DailyHistoryActor.Data]
    with ActorLogging
    with SettingsActor{

  override def preStart = {
    messageDistributor ! RegisterReceiver
  }

  startWith(Receiving, Empty)

  when(Receiving) {
    case Event(TelegramReceived(telegram), _) =>
      log.debug("Sleeping for {}", settings.history.resolution)
      goto(Sleeping) using Empty
    case Event(StateTimeout, _) =>
      // Ignored
      stay()
  }

  when(Sleeping) {
    case Event(_: TelegramReceived, _) =>
      stay()
  }

  initialize()
}
