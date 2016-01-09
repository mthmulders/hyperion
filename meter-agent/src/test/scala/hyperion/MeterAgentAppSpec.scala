package hyperion

import akka.testkit.TestProbe

class MeterAgentAppSpec extends BaseAkkaSpec {
  "Creating the MeterAgentApp" should {
    "result in creating the necessary top-level actors" in {
      new MeterAgentApp(system)
      TestProbe().expectActor("/user/meter-agent")
      TestProbe().expectActor("/user/collecting-actor")
    }
  }
}
