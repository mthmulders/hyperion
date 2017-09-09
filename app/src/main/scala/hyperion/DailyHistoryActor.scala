package hyperion

import java.time.{Duration, LocalDate, LocalDateTime}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationLong
import scala.util.{Failure, Success}

import akka.actor.{ActorLogging, ActorRef, FSM}

import hyperion.MessageDistributor.RegisterReceiver
import hyperion.DailyHistoryActor._
import hyperion.database.MeterReadingDAO.HistoricalMeterReading
import hyperion.database.MeterReadingDAO

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
                        meterReadingDAO: MeterReadingDAO)
                       (implicit executionContext: ExecutionContext)
    extends FSM[DailyHistoryActor.State, DailyHistoryActor.Data]
    with ActorLogging with AppSettings {

  override def preStart = {
    messageDistributor ! RegisterReceiver

    scheduleNextAwakening()
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

    scheduleNextAwakening()
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
        receiver ! RetrievedMeterReading(None)
    }
    stay()
  }

  private def storeMeterReading(reading: HistoricalMeterReading) = {
    log.info("Storing one record in database:")
    log.info("  Date               : {}", reading.recordDate)
    log.info("  Gas                : {}", reading.gas)
    log.info("  Electricity normal : {}", reading.electricityNormal)
    log.info("  Electricity low    : {}", reading.electricityLow)
    meterReadingDAO.recordMeterReading(reading) andThen {
      case Success(_)      => log.info("Succeeded")
      case Failure(reason) => log.error("Could not store meter reading in database: {}", reason)
    }

    stay()
  }

  private def scheduleNextAwakening() = {
    val midnight = LocalDate.now().plusDays(1).atStartOfDay()
    val untilMidnight = Duration.between(LocalDateTime.now(), midnight)
    log.info("Sleeping for {} milliseconds", untilMidnight.toMillis)
    setTimer("wake-up", StateTimeout, untilMidnight.toMillis millis, repeat = false)
  }
}
