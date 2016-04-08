package hyperion

import java.time.LocalDateTime

import scala.collection.immutable
import P1Constants._

import scala.util.Random

object TestSupport {
  private def randomBigDecimal(): BigDecimal = {
    BigDecimal(Random.nextDouble())
  }

  def randomTelegram(): P1Telegram = {
    val totalConsumption = immutable.Map(LOW_TARIFF -> randomBigDecimal(), NORMAL_TARIFF -> randomBigDecimal())
    val totalProduction = immutable.Map(LOW_TARIFF -> randomBigDecimal(), NORMAL_TARIFF -> randomBigDecimal())
    val extraDevices = immutable.Seq(P1GasMeter(1, "03", LocalDateTime.now(), randomBigDecimal()))
    P1Telegram(
      P1Header("make", "identifier"),
      P1MetaData("40", LocalDateTime.now(), "4B384547303034303436333935353037"),
      P1Data(LOW_TARIFF, randomBigDecimal(), randomBigDecimal(), totalConsumption, totalProduction, extraDevices),
      P1Checksum("checksum")
    )
  }
}
