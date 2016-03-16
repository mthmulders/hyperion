package hyperion

import java.time.LocalDateTime
import java.util.Date

import scala.collection.immutable

object P1Constants {
  val LOW_TARIFF = "0001"
  val NORMAL_TARIFF = "0002"
}

/**
  * This is the message that indicates one P1 Telegram was received from the smart meter.
  * @param telegram The telegram with data.
  */
case class TelegramReceived(telegram: P1Telegram)

/**
  * Represents a P1 Telegram.
  * @param header Header data from the smart meter
  * @param metadata Metadata about the telegram
  * @param data Actual data records
  * @param checksum A checksum value
  */
case class P1Telegram(header: P1Header,
                      metadata: P1MetaData,
                      data: P1Data,
                      checksum: P1Checksum)

/**
  * Information about the smart meter.
  * @param make Make of the meter
  * @param identification Meter identification
  */
case class P1Header(make: String,
                    identification: String)

/**
  * Metadata about a P1 telegram
  * @param versionInfo Version of the P1 spec
  * @param timestamp Timestamp when the telegram was emitted
  * @param equipmentIdentifier Equipment identifier
  */
case class P1MetaData(versionInfo: String,
                      timestamp: LocalDateTime,
                      equipmentIdentifier: String)

/**
  * Actual measurement data.
  * @param currentTariff Indicates what the current tariff is
  * @param currentConsumption Indicates what the actual consumption is
  * @param currentProduction Indicates what the actual production is
  * @param totalConsumption Indicates what the total consumption is
  * @param totalProduction Indicates what the total production is
  * @param devices (when present) contains information about devices that are connected to the smart meter
  */
case class P1Data(currentTariff: String,
                  currentConsumption: BigDecimal,
                  currentProduction: BigDecimal,
                  totalConsumption: immutable.Map[String, BigDecimal],
                  totalProduction: immutable.Map[String, BigDecimal],
                  devices: immutable.Seq[P1ExtraDevice])

/**
  * Represents data about additional devices.
  * @param id The identifier of the external device.
  */
case class P1ExtraDevice(id: Int)

case class P1Checksum(checksum: String)

case class P1GasMeterReading(timestamp: Date, gasDelivered: Double)