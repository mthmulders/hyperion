package hyperion.database

import java.time.LocalDate

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

import akka.actor.{Actor, ActorLogging, ActorRef}

import hyperion.database.DatabaseActor._

object DatabaseActor {
  case class RetrieveMeterReadingForDate(date: LocalDate)
  case class RetrievedMeterReading(reading: Option[HistoricalMeterReading])
  case class StoreMeterReading(reading: HistoricalMeterReading)
}

class DatabaseActor(meterReadingDAO: MeterReadingDAO) extends Actor with ActorLogging {
  implicit val executionContext: ExecutionContextExecutor = context.dispatcher

  override def receive: Receive = {
    case RetrieveMeterReadingForDate(date) => retrieveMeterReadingByDate(sender(), date)
    case StoreMeterReading(reading) => storeMeterReading(reading)
  }

  private def retrieveMeterReadingByDate(receiver: ActorRef, date: LocalDate) = {
    log.info(s"Retrieve meter reading for $date")

    meterReadingDAO.retrieveMeterReading(date).map(_.headOption) andThen {
      case Success(result) =>
        receiver ! RetrievedMeterReading(result)

      case Failure(reason) =>
        log.error("Error retrieving meter reading from database: {}", reason)
        receiver ! RetrievedMeterReading(None)
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
