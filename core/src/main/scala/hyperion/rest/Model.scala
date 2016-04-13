package hyperion.rest

import java.time.OffsetDateTime

/**
  * Represents one instant that the meter was read.
  *
  * @param ts The timestamp at which the meter was read
  * @param tariff The current tariff.
  * @param elecCon Current electricity consumption in kW.
  * @param elecProd Current electricity production in kW.
  * @param gas Gas meter reading in m3.
  * @param elecConsLow Low tariff electricity consumption meter reading in kWh.
  * @param elecConsNormal Normal tariff electricity consumption meter reading in kWh.
  * @param elecProdLow Low tariff electricity production meter reading in kWh.
  * @param elecProdNormal Normal tariff electricity production meter reading in kWh.
  */
case class MeterReading(ts: OffsetDateTime,
                        tariff: String,
                        elecCon: BigDecimal,
                        elecProd: BigDecimal,
                        gas: Option[BigDecimal],
                        elecConsLow: Option[BigDecimal],
                        elecConsNormal: BigDecimal,
                        elecProdLow: Option[BigDecimal],
                        elecProdNormal: BigDecimal)
