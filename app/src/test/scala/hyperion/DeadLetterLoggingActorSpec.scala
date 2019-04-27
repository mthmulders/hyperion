package hyperion

import akka.actor.DeadLetter
import akka.testkit.{EventFilter, TestProbe}

class DeadLetterLoggingActorSpec extends BaseAkkaSpec {
  private val dla = system.actorOf(DeadLetterLoggingActor.props())

  private val receiver = TestProbe()

  "Dead letters" should {

    "be logged" in {
      // Arrange
      val sender = TestProbe().ref
      val recipient = TestProbe().ref

      // Assert
      val logMessage = s"Could not deliver [message] from ${sender.path} to ${recipient.path}"
      EventFilter.info(logMessage, occurrences = 1) intercept {
        // Act
        dla ! DeadLetter("message", sender, recipient)
      }
    }
  }
}
