package hyperion.database

import java.time.{LocalDate, Month}

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

import akka.actor.{Actor, ActorLogging, ActorRef}

import hyperion.database.DatabaseActor._

object DatabaseActor {
  case class RetrieveMeterReadingForDate(date: LocalDate)
  case class RetrieveMeterReadingForMonth(month: Month, year: Int)
  case class RetrievedMeterReadings(readings: Seq[HistoricalMeterReading])
  case class StoreMeterReading(reading: HistoricalMeterReading)
}

class DatabaseActor(meterReadingDAO: MeterReadingDAO) extends Actor with ActorLogging {
  implicit val executionContext: ExecutionContextExecutor = context.dispatcher

  override def receive: Receive = {
    case RetrieveMeterReadingForDate(date) => retrieveMeterReadingByDate(sender(), date)
    case RetrieveMeterReadingForMonth(month, year) => retrieveMeterReadingsByMonth(sender(), month, year)
    case StoreMeterReading(reading) => storeMeterReading(reading)
  }

  private def retrieveMeterReadingByDate(receiver: ActorRef, date: LocalDate) = {
    log.info(s"Retrieve meter reading for $date")

    meterReadingDAO.retrieveMeterReading(date) andThen {
      case Success(Some(result)) =>
        receiver ! RetrievedMeterReadings(Seq(result))

      case Success(None) =>
        log.error(s"No meter reading for date: $date")
        receiver ! RetrievedMeterReadings(Seq.empty)

      case Failure(reason) =>
        log.error("Error retrieving meter reading from database: {}", reason)
        receiver ! RetrievedMeterReadings(Seq.empty)
    }
  }

  private def retrieveMeterReadingsByMonth(receiver: ActorRef, month: Month, year: Int) = {
    log.info(s"Retrieve meter reading for $month $year")
    val startDate = LocalDate.of(year, month, 1)
    val endDate = startDate.plusMonths(1).minusDays(1)

    meterReadingDAO.retrieveMeterReadings(startDate, endDate) andThen {
      case Success(result) if result.nonEmpty =>
        receiver ! RetrievedMeterReadings(result)

      case Success(result) if result.isEmpty =>
        log.error(s"No meter readings for month $month $year")
        receiver ! RetrievedMeterReadings(Seq.empty)

      case Failure(reason) =>
        log.error("Error retrieving meter readings from database: {}", reason)
        receiver ! RetrievedMeterReadings(Seq.empty)
    }
  }

  private def storeMeterReading(reading: HistoricalMeterReading) = {
    log.info("Storing one record in database:")
    log.info(s"  Date               : ${reading.recordDate}")
    log.info(s"  Gas                : ${reading.gas}")
    log.info(s"  Electricity normal : ${reading.electricityNormal}")
    log.info(s"  Electricity low    : ${reading.electricityLow}")

    meterReadingDAO.recordMeterReading(reading)
  }
}
