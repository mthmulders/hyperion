package hyperion

import akka.actor.{ActorIdentity, ActorRef, ActorSystem, Identify}
import akka.testkit.{EventFilter, TestEvent, TestProbe}
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{BeforeAndAfterAll, Matchers}
import org.slf4j.LoggerFactory

import scala.collection.{immutable, mutable}
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}

abstract class BaseAkkaSpec extends BaseSpec with Matchers with BeforeAndAfterAll with TypeCheckedTripleEquals {
  protected[this] val log = LoggerFactory.getLogger(getClass)

  implicit class TestProbeOps(probe: TestProbe) {
    private val maxWaitForActor = 100 milliseconds

    def expectActor(path: String, max: FiniteDuration = maxWaitForActor): immutable.Seq[ActorRef] = {
      val actors = mutable.ArrayBuffer.empty[ActorRef]
      (probe.system actorSelection path).tell(Identify(path), probe.ref)
      probe.receiveWhile(max) {
        case ActorIdentity(`path`, Some(ref)) => actors += ref
        case ActorIdentity(`path`, None) => fail(s"Expected Some(ActorRef) for path $path but got None")
      }
      immutable.Seq(actors:_*)
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
