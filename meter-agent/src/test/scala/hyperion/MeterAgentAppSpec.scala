package hyperion

import akka.testkit.TestProbe

import scala.concurrent.duration.DurationInt

class MeterAgentAppSpec extends BaseAkkaSpec {
  "Creating the MeterAgentApp" should {
    "result in creating the necessary top-level actors" in {
      new MeterAgentApp(system)
      TestProbe().expectActor("/user/meter-agent", 250 milliseconds)
      TestProbe().expectActor("/user/collecting-actor", 250 milliseconds)
    }
  }
}
