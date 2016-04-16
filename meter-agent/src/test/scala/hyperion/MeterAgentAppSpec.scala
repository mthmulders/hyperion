package hyperion

import akka.testkit.TestProbe

import scala.concurrent.duration.DurationInt

class MeterAgentAppSpec extends BaseAkkaSpec {
  "Creating the MeterAgentApp" should {
    "result in creating the necessary top-level actors" in {
      new MeterAgentApp(system)

      val createdActors = TestProbe().expectActor("/user/*", 2 seconds)
      createdActors should have size 3
      val createdPaths = createdActors.map(_.path.toString).map(_.replace("akka://default/user/", ""))
      createdPaths should contain allOf ("meter-agent", "collecting-actor", "forwarding-actor")
    }
  }
}
