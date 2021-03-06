package hyperion

import akka.actor.{ActorIdentity, ActorRef, ActorSystem, Identify}
import akka.testkit.{EventFilter, ImplicitSender, TestEvent, TestKit, TestProbe}
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.{immutable, mutable}
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

abstract class BaseAkkaSpec extends TestKit(ActorSystem())
  with Core
  with ImplicitSender
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with TypeCheckedTripleEquals {

  protected[this] val log: Logger = LoggerFactory.getLogger(getClass)

  implicit class TestProbeOps(probe: TestProbe) {
    private val maxWaitForActor = 100 milliseconds

    def expectActor(path: String, max: FiniteDuration = maxWaitForActor): immutable.Seq[ActorRef] = {
      val actors = mutable.ArrayBuffer.empty[ActorRef]
      (probe.system actorSelection path).tell(Identify(path), probe.ref)
      probe.receiveWhile(max) {
        case ActorIdentity(`path`, Some(ref)) => actors += ref
        case ActorIdentity(`path`, None) => fail(s"Expected Some(ActorRef) for path $path but got None")
        case ActorIdentity(otherPath, _) => log.warn(s"Expected Some(ActorRef) for path $path but got one for path $otherPath")
        case msg: Any => log.warn(s"Expected Some(ActorRef) for path $path but got $msg")
      }
      actors.toSeq
    }
  }

  system.eventStream.publish(TestEvent.Mute(EventFilter.debug()))
  system.eventStream.publish(TestEvent.Mute(EventFilter.info()))
  system.eventStream.publish(TestEvent.Mute(EventFilter.warning()))
  system.eventStream.publish(TestEvent.Mute(EventFilter.error()))

  override protected def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }
}
