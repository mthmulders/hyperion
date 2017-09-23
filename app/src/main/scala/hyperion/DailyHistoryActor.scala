package hyperion

import java.time.{Duration, LocalDate, LocalDateTime}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationLong

import akka.actor.{ActorLogging, ActorRef, FSM}

import hyperion.MessageDistributor.RegisterReceiver
import hyperion.DailyHistoryActor._
import hyperion.database.DatabaseActor.StoreMeterReading
import hyperion.database.HistoricalMeterReading
import hyperion.p1._
import hyperion.p1.TelegramReceived

object DailyHistoryActor {
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
  * @param databaseActor The Actor that interacts with the database.
  */
class DailyHistoryActor(messageDistributor: ActorRef,
                        databaseActor: ActorRef)
                       (implicit executionContext: ExecutionContext)
    extends FSM[DailyHistoryActor.State, DailyHistoryActor.Data]
    with ActorLogging with AppSettings {

  override def preStart: Unit = {
    messageDistributor ! RegisterReceiver

    scheduleNextAwakening()
  }

  startWith(Sleeping, Empty)

  when(Receiving) {
    case Event(TelegramReceived(telegram), _) => prepareMeterReading(telegram)
    case Event(StateTimeout, _)               => stay()
  }

  when(Sleeping) {
    case Event(_: TelegramReceived, _)        => stay()
    case Event(StateTimeout, _)               => log.debug("Awaking to receive new meter reading"); goto(Receiving)
  }

  initialize()

  private def prepareMeterReading(telegram: P1Telegram) = {
    val today = LocalDate.now()
    val gas = telegram.data.devices.find(_.isInstanceOf[P1GasMeter]).map(_.asInstanceOf[P1GasMeter].gasDelivered).orNull
    val electricityNormal = telegram.data.totalConsumption(P1Constants.normalTariff)
    val electricityLow = telegram.data.totalConsumption(P1Constants.lowTariff)

    log.info("Scheduling database I/O")
    databaseActor ! StoreMeterReading(HistoricalMeterReading(today, gas, electricityNormal, electricityLow))

    scheduleNextAwakening()
    goto(Sleeping) using Empty
  }

  private def scheduleNextAwakening(): Unit = {
    val midnight = LocalDate.now().plusDays(1).atStartOfDay()
    val untilMidnight = Duration.between(LocalDateTime.now(), midnight)
    log.info("Sleeping for {} milliseconds", untilMidnight.toMillis)
    setTimer("wake-up", StateTimeout, untilMidnight.toMillis millis, repeat = false)
  }
}
