package hyperion.rest

import hyperion.{BaseSpec, TestSupport}
import hyperion.p1.{P1Constants, P1GasMeter}
import org.scalatest.OptionValues

class HyperionConversionsSpec extends BaseSpec with OptionValues with HyperionConversions {
  "The telegramWrapper" should {
    "convert a P1 telegram in a MeterReading object" in {
      // Arrange
      val telegram = TestSupport.randomTelegram()

      // Act
      val result: MeterReading = telegram

      // Assert
      result.elecConsLow.value should be(telegram.data.totalConsumption(P1Constants.lowTariff))
      result.elecConsNormal should be(telegram.data.totalConsumption(P1Constants.normalTariff))

      result.elecProdLow.value should be(telegram.data.totalProduction(P1Constants.lowTariff))
      result.elecProdNormal should be(telegram.data.totalProduction(P1Constants.normalTariff))

      result.elecCon should be(telegram.data.currentConsumption)
      result.elecProd should be(telegram.data.currentProduction)
      result.tariff should be(telegram.data.currentTariff)

      result.ts should be(telegram.metadata.timestamp)

      result.gas should be(telegram.data.devices
        .find(_.isInstanceOf[P1GasMeter])
        .map(_.asInstanceOf[P1GasMeter].gasDelivered))
    }
  }
}
