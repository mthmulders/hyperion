package hyperion.rest

import hyperion.p1.{P1Constants, P1ExtraDevice, P1GasMeter, P1Telegram}

/**
  * Provides implicit conversions to data structures for the REST API.
  */
trait HyperionConversions {
  /**
    * Converts a [[P1Telegram]] to a [[MeterReading]]
    * @param telegram The [[P1Telegram]]
    * @return a [[MeterReading]] value
    */
  implicit def p1Telegram2MeterReading(telegram: P1Telegram): MeterReading = {
    val gasUsage: PartialFunction[P1ExtraDevice, BigDecimal] = {
      case P1GasMeter(_, _, _, gasDelivered) => gasDelivered
    }
    val gasConsumption: Option[BigDecimal] = telegram.data.devices.collect(gasUsage).reduceOption(_ + _)

    MeterReading(
      telegram.metadata.timestamp,
      telegram.data.currentTariff,
      telegram.data.currentConsumption,
      telegram.data.currentProduction,
      gasConsumption,
      telegram.data.totalConsumption.get(P1Constants.lowTariff),
      telegram.data.totalConsumption(P1Constants.normalTariff),
      telegram.data.totalProduction.get(P1Constants.lowTariff),
      telegram.data.totalProduction(P1Constants.normalTariff)
    )
  }
}
