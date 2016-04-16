package hyperion

import java.time.OffsetDateTime
import java.util.Date

import scala.collection.immutable

object P1Constants {
  val lowTariff = "0001"
  val normalTariff = "0002"
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
                      checksum: String)

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
                      timestamp: OffsetDateTime,
                      equipmentIdentifier: String)

/**
  * Actual measurement data.
  * @param currentTariff Indicates what the current tariff is
  * @param currentConsumption Indicates what the actual consumption is (kW).
  * @param currentProduction Indicates what the actual production is (kW).
  * @param totalConsumption Indicates what the total consumption is per tariff (kWh).
  * @param totalProduction Indicates what the total production is per tariff (kWh).
  * @param devices (when present) contains information about devices that are connected to the smart meter
  */
case class P1Data(currentTariff: String,
                  currentConsumption: BigDecimal,
                  currentProduction: BigDecimal,
                  totalConsumption: immutable.Map[String, BigDecimal],
                  totalProduction: immutable.Map[String, BigDecimal],
                  devices: immutable.Seq[P1ExtraDevice])

/**
  * Base class for records that represent data about additional devices.
  */
abstract class P1ExtraDevice {
  /* The identifier of the external device. */
  def id: Int
}

/**
  * Represents data about an unsupported external device.
  * @param id The identifier of the external device.
  * @param deviceType Type of device (no code table known yet).
  */
case class P1UnknownDevice(id: Int, deviceType: String) extends P1ExtraDevice

/**
  * Represents data about an external gas meter.
  * @param id The identifier of the external device.
  * @param equipmentIdentifier Equipment identifier for the external gas meter.
  * @param lastCapture Last moment data was captured from the gas meter.
  * @param gasDelivered Amount of gas delivered
  */
case class P1GasMeter(id: Int,
                      equipmentIdentifier: String,
                      lastCapture: OffsetDateTime,
                      gasDelivered: BigDecimal) extends P1ExtraDevice
