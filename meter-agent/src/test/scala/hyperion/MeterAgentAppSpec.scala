package hyperion

import akka.testkit.TestProbe

class MeterAgentAppSpec extends BaseAkkaSpec {
  "Creating the MeterAgentApp" should {
    "result in creating a top-level actor named 'meter-agent'" in {
      new MeterAgentApp(system)
      TestProbe().expectActor("/user/meter-agent")
    }
  }
}
