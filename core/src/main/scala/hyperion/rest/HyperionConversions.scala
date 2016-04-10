package hyperion.rest

import hyperion.{P1Constants, P1GasMeter, P1Telegram}

/**
  * Provides implicit conversions to data structures for the REST API.
  */
object HyperionConversions {
  /**
    * Converts a [[P1Telegram]] to a [[MeterReading]]
    * @param telegram The [[P1Telegram]]
    * @return a [[MeterReading]] value
    */
  implicit def telegramWrapper(telegram: P1Telegram): MeterReading = {
    val gasConsumption = telegram.data.devices
      .find(_.isInstanceOf[P1GasMeter])
      .map(_.asInstanceOf[P1GasMeter].gasDelivered)

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
