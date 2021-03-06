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
  final case class UsageDataRecord(date: LocalDate, gas: BigDecimal, electricityNormal: BigDecimal, electricityLow: BigDecimal)

  final case class CalculateUsage(start: LocalDate, end: LocalDate)

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
        case Failure(throwable) => log.error(throwable, s"Could not retrieve readings from $start to $end")
      }
  }

  private def combineMeterReadingsToUsageData(records: Seq[HistoricalMeterReading]): Seq[UsageDataRecord] = {
    log.debug(s"Retrieved ${records.length} records as input for calculation")

    val hasData = records.lengthCompare(2) >= 0
    if (hasData) {
      records.sliding(2).map(recs => {
        val former +: latter +: _ = recs
        UsageDataRecord(
          former.recordDate,
          latter.gas - former.gas,
          latter.electricityNormal - former.electricityNormal,
          latter.electricityLow - former.electricityLow
        )
      }).toSeq
    } else {
      log.warning("Not enough data available to calculate usage")
      Seq.empty
    }
  }
}
