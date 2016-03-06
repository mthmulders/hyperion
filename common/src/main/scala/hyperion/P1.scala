package hyperion

import java.time.LocalDateTime
import java.util.Date

import scala.collection.immutable

case class P1Telegram(header: P1Header,
                      metadata: P1MetaData,
                      data: P1Data,
                      checksum: P1Checksum)

case class P1Header(make: String,
                    identification: String)

case class P1MetaData(versionInfo: String,
                      timestamp: LocalDateTime,
                      equipmentIdentifier: String)

case class P1Data(currentTariff: String,
                  currentConsumption: BigDecimal,
                  currentProduction: BigDecimal,
                  totalConsumption: immutable.Map[String, BigDecimal],
                  totalProduction: immutable.Map[String, BigDecimal],
                  devices: Option[immutable.Seq[P1ExtraDevice]])

case class P1ExtraDevice()

case class P1Checksum(checksum: String)

case class P1GasMeterReading(timestamp: Date, gasDelivered: Double)