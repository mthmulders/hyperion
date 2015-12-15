package hyperion

import akka.actor.{ActorIdentity, Identify, ActorRef, ActorSystem}
import akka.testkit.{TestProbe, EventFilter, TestEvent}
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{Matchers, BeforeAndAfterAll}

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}

abstract class BaseAkkaSpec extends BaseSpec with Matchers with BeforeAndAfterAll with TypeCheckedTripleEquals {
  implicit class TestProbeOps(probe: TestProbe) {
    private val maxWaitForActor = 100 milliseconds

    def expectActor(path: String, max: FiniteDuration = maxWaitForActor): ActorRef = {
      probe.within(max) {
        var actor = null: ActorRef
        probe.awaitAssert {
          (probe.system actorSelection path).tell(Identify(path), probe.ref)
          probe.expectMsgPF(100 milliseconds) {
            case ActorIdentity(`path`, Some(ref)) => actor = ref
          }
        }
        actor
      }
    }
  }

  implicit val system = ActorSystem()
  system.eventStream.publish(TestEvent.Mute(EventFilter.debug()))
  system.eventStream.publish(TestEvent.Mute(EventFilter.info()))
  system.eventStream.publish(TestEvent.Mute(EventFilter.warning()))
  system.eventStream.publish(TestEvent.Mute(EventFilter.error()))

  override protected def afterAll(): Unit = {
    system.terminate()
    Await.result(system.whenTerminated, Duration.Inf)
  }
}
