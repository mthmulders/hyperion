package hyperion.rest

import java.time.LocalDate

import hyperion.BaseSpec
import hyperion.database.HistoricalMeterReading

class HyperionJsonProtocolSpec extends BaseSpec with HyperionJsonProtocol {
  "A historical meter reading from the database" should {
    "be convertible to a JSON structure" in {
      // Arrange
      val input = HistoricalMeterReading(LocalDate.now(), BigDecimal(1), BigDecimal(2), BigDecimal(3))

      // Act
      val result = historicalMeterReadingFormat.write(input).toString()

      // Assert
      result should include(s""""recordDate":"${input.recordDate}"""")
      result should include(s""""gas":${input.gas}""")
      result should include(s""""electricityLow":${input.electricityLow}""")
      result should include(s""""electricityNormal":${input.electricityNormal}""")
    }
  }
}
