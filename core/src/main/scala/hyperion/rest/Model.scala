package hyperion.rest

import java.time.LocalDateTime

/**
  * Represents one instant that the meter was read.
  *
  * @param ts The timestamp at which the meter was read
  * @param tariff The current tariff (if applicable).
  * @param consumption Current electricity consumption.
  * @param production Current electricity production.
  */
case class MeterReading(ts: LocalDateTime,
                        tariff: String,
                        consumption: BigDecimal,
                        production: BigDecimal)