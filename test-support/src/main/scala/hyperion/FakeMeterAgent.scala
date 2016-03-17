package hyperion

import java.time.LocalDateTime

import akka.actor.{Address, ActorSystem, RootActorPath}

import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.StdIn
import scala.util.Random
import P1Constants._

object FakeMeterAgent extends App {
  val system = ActorSystem("hyperion-system")
  val path = RootActorPath(Address("akka.tcp", system.name, "localhost", 2552)) / "user" / "receiver"
  val selection = system.actorSelection(path)

  println("Enter to trigger a new telegram, 'q' followed by enter quits")
  while (!"q".equals(StdIn.readLine())) {
    selection ! TelegramReceived(randomTelegram())
  }
  Await.result(system.terminate(), Duration.Inf)

  private def randomTelegram(): P1Telegram = {
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

  private def randomBigDecimal(): BigDecimal = {
    BigDecimal(Random.nextDouble())
  }
}
