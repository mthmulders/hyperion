package hyperion

import java.time.LocalDate

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration.DurationInt
import hyperion.UsageCalculationActor.{CalculateUsage, UsageDataRecord}
import hyperion.database.DatabaseActor.{RetrieveMeterReadingForDateRange, RetrievedMeterReadings}
import hyperion.database.HistoricalMeterReading

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

object UsageCalculationActor {
  case class UsageDataRecord(date: LocalDate, gas: BigDecimal, electNormal: BigDecimal, electLow: BigDecimal)

  case class CalculateUsage(start: LocalDate, end: LocalDate)

  def props(database: ActorRef): Props = {
    Props(new UsageCalculationActor(database))
  }
}

class UsageCalculationActor(database: ActorRef) extends Actor with ActorLogging {
  implicit val executionContext: ExecutionContextExecutor = context.dispatcher
  implicit val timeout: Timeout = Timeout(3 seconds)

  override def receive: Receive = {
    case CalculateUsage(start, end) => calculateUsage(sender(), start, end)
  }

  private def calculateUsage(sender: ActorRef, start: LocalDate, end: LocalDate): Unit = {
    log.info(s"Calculating usage between $start and $end")
    (database ? RetrieveMeterReadingForDateRange(start, end))
      .mapTo[RetrievedMeterReadings]
      .onComplete {
        case Success(data)      => sender ! combineMeterReadingsToUsageData(data.readings)
        case Failure(throwable) => log.error(s"Could not retrieve readings from $start to $end", throwable)
      }
  }

  private def combineMeterReadingsToUsageData(records: Seq[HistoricalMeterReading]): Seq[UsageDataRecord] = {
    log.debug(s"Retrieved ${records.length} records as input for calculation")

    records.sliding(2).map({
      case Seq(former, latter) =>
        UsageDataRecord(
          former.recordDate,
          latter.gas - former.gas,
          latter.electricityNormal - former.electricityNormal,
          latter.electricityLow - former.electricityLow
        )
    }).toSeq
  }
}
