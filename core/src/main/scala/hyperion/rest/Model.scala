package hyperion.rest

import java.time.LocalDateTime

/**
  * Represents one instant that the meter was read.
  *
  * @param ts The timestamp at which the meter was read
  * @param tariff The current tariff.
  * @param consumption Current electricity consumption in kW.
  * @param production Current electricity production in kW.
  * @param gas Gas consumption (last measurement) in m3.
  */
case class MeterReading(ts: LocalDateTime,
                        tariff: String,
                        consumption: BigDecimal,
                        production: BigDecimal,
                        gas: Option[BigDecimal])