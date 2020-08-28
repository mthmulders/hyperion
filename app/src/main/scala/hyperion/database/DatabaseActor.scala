package hyperion.database

import java.sql.SQLException
import java.time.{LocalDate, Month}

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}
import akka.actor.{Actor, ActorLogging, ActorRef}
import hyperion.AppSettings
import hyperion.database.DatabaseActor._
import slick.jdbc.JdbcBackend.Database

object DatabaseActor {
  final case object GetDatabaseInfo
  final case class RetrieveMeterReadingForDate(date: LocalDate)
  final case class RetrieveMeterReadingForDateRange(start: LocalDate, end: LocalDate)
  final case class RetrieveMeterReadingForMonth(month: Month, year: Int)
  final case class RetrievedMeterReadings(readings: Seq[HistoricalMeterReading])
  final case class StoreMeterReading(reading: HistoricalMeterReading)
}

class DatabaseActor extends Actor with ActorLogging with AppSettings {
  implicit val executionContext: ExecutionContextExecutor = context.dispatcher

  log.info("Connecting to database...")
  private[this] val db = Database.forConfig("hyperion.database")

  private[this] val dao = createDao()
  protected def createDao() = new MeterReadingDAO(db)

  private[this] val session = db.createSession()
  log.info("Database connection established: {} {}",
    session.metaData.getDatabaseProductName: Any,
    session.metaData.getDatabaseProductVersion: Any
  )

  override def postStop(): Unit = {
    session.close()
    db.close()
  }

  override def receive: Receive = {
    case GetDatabaseInfo => getDatabaseInfo(sender())
    case RetrieveMeterReadingForDate(date) => retrieveMeterReadingByDate(sender(), date)
    case RetrieveMeterReadingForDateRange(start, end) => retrieveMeterReadingByDateRange(sender(), start, end)
    case RetrieveMeterReadingForMonth(month, year) => retrieveMeterReadingsByMonth(sender(), month, year)
    case StoreMeterReading(reading) => storeMeterReading(reading)
  }

  private def getDatabaseInfo(receiver: ActorRef) = {
    log.info("Retrieving database metadata")
    val metadata = session.metaData
    receiver ! s"${metadata.getDatabaseProductName} ${metadata.getDatabaseProductVersion}"
  }

  private def retrieveMeterReadingByDateRange(receiver: ActorRef, start: LocalDate, end: LocalDate) = {
    log.info(s"Retrieve meter reading from $start to $end")

    dao.retrieveMeterReadings(start, end) andThen {
      case Success(result) if result.nonEmpty =>
        receiver ! RetrievedMeterReadings(result)

      case Success(result) if result.isEmpty =>
        log.error(s"No meter readings between $start and $end")
        receiver ! RetrievedMeterReadings(Seq.empty)

      case Failure(reason) =>
        log.error("Error retrieving meter readings from database: {}", reason)
        receiver ! RetrievedMeterReadings(Seq.empty)
    }
  }

  private def retrieveMeterReadingByDate(receiver: ActorRef, date: LocalDate) = {
    log.info(s"Retrieve meter reading for $date")

    dao.retrieveMeterReading(date) andThen {
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

    retrieveMeterReadingByDateRange(receiver, startDate, endDate)
  }

  private def storeMeterReading(reading: HistoricalMeterReading) = {
    log.info("Storing one record in database:")
    log.info(s"  Date               : ${reading.recordDate}")
    log.info(s"  Gas                : ${reading.gas}")
    log.info(s"  Electricity normal : ${reading.electricityNormal}")
    log.info(s"  Electricity low    : ${reading.electricityLow}")

    dao.recordMeterReading(reading)
      .recover { case e: SQLException => scheduleRetry(e, reading) }
  }

  private def scheduleRetry(cause: Throwable, reading: HistoricalMeterReading): Unit = {
    log.error(s"Inserting failed due to ${cause.getMessage}, retrying in an hour...")
    context.system.scheduler.scheduleOnce(1 hour, self, StoreMeterReading(reading))
  }
}
