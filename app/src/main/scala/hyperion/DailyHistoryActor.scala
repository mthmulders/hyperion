package hyperion

import java.time.{Duration, LocalDate, LocalDateTime}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationLong
import scala.util.{Failure, Success}

import akka.actor.{ActorLogging, ActorRef, FSM}
import hyperion.MessageDistributor.RegisterReceiver
import hyperion.DailyHistoryActor._
import hyperion.database.MeterReadingDAO.HistoricalMeterReading
import hyperion.database.{DatabaseSupport, MeterReadingDAO}

object DailyHistoryActor {
  sealed trait State
  case object Sleeping extends State
  case object Receiving extends State

  sealed trait Data
  case object Empty extends Data

  case class StoreMeterReading(reading: MeterReadingDAO.HistoricalMeterReading)
  case class RetrieveMeterReading(date: LocalDate)
  case class RetrievedMeterReading(reading: Option[MeterReadingDAO.HistoricalMeterReading])
}

/**
  * Actor that stores a daily meter reading in an external database
  *
  * @param messageDistributor The Actor that distributes incoming telegrams.
  * @param meterReadingDAO DAO for interacting with the database.
  */
class DailyHistoryActor(messageDistributor: ActorRef,
                        meterReadingDAO: MeterReadingDAO,
                        settings: AppSettings)
                       (implicit executionContext: ExecutionContext)
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

    scheduleAwakenings()
  }

  startWith(Sleeping, Empty)

  when(Receiving) {
    case Event(TelegramReceived(telegram), _) => prepareMeterReading(telegram)
    case Event(StoreMeterReading(reading), _) => storeMeterReading(reading)
    case Event(RetrieveMeterReading(date), _) => retrieveMeterReading(sender(), date)
    case Event(StateTimeout, _)               => stay()
  }

  when(Sleeping) {
    case Event(_: TelegramReceived, _)        => stay()
    case Event(StoreMeterReading(reading), _) => storeMeterReading(reading)
    case Event(RetrieveMeterReading(date), _) => retrieveMeterReading(sender(), date)
    case Event(StateTimeout, _)               => log.debug("Awaking to receive new meter reading"); goto(Receiving)
  }

  initialize()

  private def prepareMeterReading(telegram: P1Telegram) = {
    val today = LocalDate.now()
    val gas = telegram.data.devices.find(_.isInstanceOf[P1GasMeter]).map(_.asInstanceOf[P1GasMeter].gasDelivered).orNull
    val electricityNormal = telegram.data.totalConsumption(P1Constants.normalTariff)
    val electricityLow = telegram.data.totalConsumption(P1Constants.lowTariff)

    log.info("Scheduling database I/O")
    self ! StoreMeterReading(HistoricalMeterReading(today, gas, electricityNormal, electricityLow))

    log.debug("Sleeping for {}", settings.daily.resolution)
    goto(Sleeping) using Empty
  }

  private def retrieveMeterReading(receiver: ActorRef, date: LocalDate) = {
    meterReadingDAO.retrieveMeterReading(date).andThen {
      case Success(result) =>
        if (result.size > 1) log.warning("Expected one meter reading, got {}", result.size)
        if (result.isEmpty)  log.info("No meter reading found for date {}", date)

        receiver ! RetrievedMeterReading(result.headOption)

      case Failure(reason) =>
        log.error("Error retrieving meter reading from database: {}", reason)
    }
    stay()
  }

  private def storeMeterReading(reading: HistoricalMeterReading) = {
    log.info("Storing one record in database:")
    log.info("  Date               : {}", reading.recordDate)
    log.info("  Gas                : {}", reading.gas)
    log.info("  Electricity normal : {}", reading.electricityNormal)
    log.info("  Electricity low    : {}", reading.electricityLow)
    meterReadingDAO.recordMeterReading(reading)

    stay()
  }

  private def scheduleAwakenings() = {
    val tomorrowMidnight = LocalDate.now().plusDays(1).atStartOfDay()
    val untilMidnight = Duration.between(LocalDateTime.now(), tomorrowMidnight)
    log.info("Sleeping for {} milliseconds", untilMidnight.toMillis)
    setTimer("initial-daily-awake", StateTimeout, 1000 millis, repeat = false)

    setTimer("repeating-daily-awake", StateTimeout, settings.daily.resolution, repeat = true)
  }
}
