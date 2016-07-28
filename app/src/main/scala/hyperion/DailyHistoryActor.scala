package hyperion

import java.time.{Duration, LocalDate, LocalDateTime}

import scala.concurrent.duration.DurationLong

import akka.actor.{ActorLogging, ActorRef, FSM, Props}
import hyperion.MessageDistributor.RegisterReceiver
import hyperion.DailyHistoryActor._
import hyperion.database.{DatabaseSupport, MeterReadingDAO}

object DailyHistoryActor {
  sealed trait State
  case object Sleeping extends State
  case object Receiving extends State

  sealed trait Data
  case object Empty extends Data

  case class StoreMeterReading(reading: MeterReadingDAO.MeterReading)
}

/**
  * Actor that stores a daily meter reading in an external database
  *
  * @param messageDistributor The Actor that distributes incoming telegrams.
  */
class DailyHistoryActor(messageDistributor: ActorRef, settings: AppSettings)
    extends FSM[DailyHistoryActor.State, DailyHistoryActor.Data]
    with ActorLogging
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
      log.info("Scheduling database I/O")
      self ! StoreMeterReading((today, gas, electricityNormal, electricityLow))
      log.debug("Sleeping for {}", settings.daily.resolution)
      goto(Sleeping) using Empty

    case Event(e @ StoreMeterReading(reading), _) =>
      storeMeterReading(e)
    case Event(StateTimeout, _) =>
      // Ignored
      stay()
  }

  when(Sleeping) {
    case Event(_: TelegramReceived, _) =>
      stay()
    case Event(e @ StoreMeterReading(reading), _) =>
      storeMeterReading(e)
    case Event(StateTimeout, _) =>
      log.debug("Awaking to receive new meter reading")
      goto(Receiving) using Empty
  }

  private def storeMeterReading(event: StoreMeterReading) = {
    val reading = event.reading
    log.info("Storing one record in database:")
    log.info("  Date               : {}", reading._1)
    log.info("  Gas                : {}", reading._2)
    log.info("  Electricity normal : {}", reading._3)
    log.info("  Electricity low    : {}", reading._4)
    MeterReadingDAO.recordMeterReading(reading)

    stay()
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
