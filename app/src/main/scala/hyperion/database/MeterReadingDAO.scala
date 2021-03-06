package hyperion.database

import java.time.LocalDate

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

import org.slf4j.LoggerFactory
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.PostgresProfile.api._

final case class HistoricalMeterReading (
  recordDate: LocalDate,
  gas: BigDecimal,
  electricityNormal: BigDecimal,
  electricityLow: BigDecimal
)

class MeterReadingDAO(db: Database) extends DateTimeColumns {
  private[this] val log = LoggerFactory.getLogger(getClass)

  private class MeterReadings(tag: Tag) extends Table[HistoricalMeterReading](tag, "meter_readings") {
    def recordDate = column[LocalDate]("record_date", O.PrimaryKey)
    def gas = column[BigDecimal]("gas")
    def electricityNormal = column[BigDecimal]("electricity_normal")
    def electricityLow = column[BigDecimal]("electricity_low")

    def * = (recordDate, gas, electricityNormal, electricityLow) <> (HistoricalMeterReading.tupled, HistoricalMeterReading.unapply)
  }

  private[this] val meterReadings = TableQuery[MeterReadings]

  def recordMeterReading(value: HistoricalMeterReading): Future[Int] = {
    db.run(meterReadings += value) andThen {
      case Failure(reason) =>
        log.error(s"Could not store meter reading in database: ${reason.getMessage}", reason)
      case Success(count) =>
        log.info(s"Successfully stored meter $count reading(s) into database")
    }
  }

  def retrieveMeterReading(date: LocalDate): Future[Option[HistoricalMeterReading]] = {
    val query = meterReadings.filter(_.recordDate === date)
    db.run(query.result) map (_.headOption)
  }

  def retrieveMeterReadings(startDate: LocalDate, endDate: LocalDate): Future[Seq[HistoricalMeterReading]] = {
    val query = meterReadings.filter(_.recordDate.between(startDate, endDate))
    db.run(query.result) map(a => a.toSeq)
  }
}
