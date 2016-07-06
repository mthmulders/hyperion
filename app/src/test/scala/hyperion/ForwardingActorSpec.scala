package hyperion

import akka.actor.PoisonPill
import akka.testkit.{TestFSMRef, TestProbe}

class ForwardingActorSpec extends BaseAkkaSpec {
  private val target = TestProbe().ref
  private val probe = TestProbe()

  "When forwarding" should {
    "watch its target" in {
      // Arrange
      val target = TestProbe().ref
      val selection = system.actorSelection(target.path)
      val fsm = TestFSMRef(new ForwardingActor(selection), "watch-target")
      probe watch fsm
      fsm.setState(Forwarding, TargetFound(target))

      // Act
      target ! PoisonPill

      // Assert
      fsm.stateName shouldBe Waiting
      fsm.stateData shouldBe a [Queue]
      fsm.stateData.asInstanceOf[Queue].retries shouldBe 10
    }
  }

  "When waiting" should {
    "enqueue messages" in {
      // Arrange
      val target = TestProbe().ref
      val fsm = TestFSMRef(new ForwardingActor(system.actorSelection("/system/non-existing")), "enqueue-messages")
      fsm.setState(Waiting, Queue(Vector.empty[Any], 10))
      val msg = ""

      // Act
      fsm ! msg

      // Assert
      fsm.stateName shouldBe Waiting
      fsm.stateData shouldBe a [Queue]
      val q = fsm.stateData.asInstanceOf[Queue].q should contain (msg)
    }

    "send enqueued messages when target is found" in {
      // Arrange
      val target = TestProbe()
      val fsm = TestFSMRef(new ForwardingActor(system.actorSelection(target.ref.path)), "send-enqueued-messages")
      val msg = "msg"

      // Act
      fsm ! msg
      fsm.setState(Forwarding, TargetFound(target.ref))

      // Assert
      target.expectMsg(msg)
    }
  }
}
