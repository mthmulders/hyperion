package hyperion

import java.time.{Duration, LocalDate, LocalDateTime}

import scala.concurrent.duration.DurationLong

import akka.actor.{ActorLogging, ActorRef, FSM, Props}
import hyperion.MessageDistributor.RegisterReceiver
import hyperion.DailyHistoryActor._
import hyperion.database.{DatabaseSupport, MeterReadingDAO}

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
    with SettingsActor
    with DatabaseSupport {

  override def preStart = {
    messageDistributor ! RegisterReceiver

    log.info("Connected to database {} {}.{} at {}",
      session.metaData.getDatabaseProductName,
      session.metaData.getDatabaseMajorVersion,
      session.metaData.getDatabaseMinorVersion,
      session.metaData.getURL)
  }

  startWith(Sleeping, Empty)

  when(Receiving) {
    case Event(TelegramReceived(telegram), _) =>
      val today = LocalDate.now()
      val gas = telegram.data.devices.find(_.isInstanceOf[P1GasMeter]).map(_.asInstanceOf[P1GasMeter].gasDelivered).orNull
      val electricityNormal = telegram.data.totalConsumption(P1Constants.normalTariff)
      val electricityLow = telegram.data.totalConsumption(P1Constants.lowTariff)

      log.info("Storing one record in database:")
      log.info("  Date               : {}", today)
      log.info("  Gas                : {}", gas)
      log.info("  Electricity normal : {}", electricityNormal)
      log.info("  Electricity low    : {}", electricityLow)
      MeterReadingDAO.recordMeterReading((today, gas, electricityNormal, electricityLow))

      log.debug("Sleeping for {}", settings.daily.resolution)
      goto(Sleeping) using Empty
    case Event(StateTimeout, _) =>
      // Ignored
      stay()
  }

  when(Sleeping) {
    case Event(_: TelegramReceived, _) =>
      stay()
    case Event(StateTimeout, _) =>
      log.debug("Awaking to receive new meter reading")
      goto(Receiving) using Empty
  }

  def scheduleAwakenings() = {
    val tomorrowMidnight = LocalDate.now().plusDays(1).atStartOfDay()
    val untilMidnight = Duration.between(LocalDateTime.now(), tomorrowMidnight)
    log.info("Sleeping for {} milliseconds", untilMidnight.toMillis)
    setTimer("initial-daily-awake", StateTimeout, untilMidnight.toMillis millis, repeat = false)

    setTimer("repeating-daily-awake", StateTimeout, settings.daily.resolution, repeat = true)
  }

  scheduleAwakenings()

  initialize()
}
