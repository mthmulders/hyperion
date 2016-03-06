package hyperion

import scala.collection.immutable
import scala.concurrent.duration.DurationInt
import akka.actor._

object ForwardingActor {
  def props(target: ActorSelection): Props = {
    Props(new ForwardingActor(target))
  }
}

sealed trait State
case object Waiting extends State
case object Forwarding extends State
sealed trait Data
case class TargetFound(target: ActorRef) extends Data
case class Queue(q: immutable.Vector[Any], retries: Int) extends Data

/**
  * This Actor forwards all messages it receives to a remote Actor, identified by.
  *
  * @param selection the remote Actor.
  */
class ForwardingActor(selection: ActorSelection) extends FSM[State, Data] with ActorLogging with Stash {
  private[this] val maxRetries = 10
  private[this] val messageId = hashCode()

  askSelectionForIdentification()

  startWith(Waiting, Queue(Vector.empty[Any], maxRetries))

  when(Waiting) {
    case Event(ActorIdentity(`messageId`, Some(ref)), Queue(q, _)) =>
      log.info("Received identity for remote target: {}", ref)
      context.watch(ref)
      q.foreach(ref ! _)
      unstashAll()
      goto(Forwarding) using TargetFound(ref)
    case Event(ActorIdentity(`messageId`, None), Queue(q, retries)) if retries > 0 =>
      log.warning("Got not found when asking for {}, backing off", selection.toString())
      stay()
    case Event(msg, Queue(q, retries)) =>
      log.debug("Enqueuing message {}", msg)
      stay() using Queue(q :+ msg, retries)
  }

  when(Waiting, 5 seconds) {
    case Event(StateTimeout, Queue(q, retries)) if retries > 0 =>
      log.warning("Could not find requested target {}, retrying ({} attempts left)", selection.toString(), retries)
      askSelectionForIdentification()
      goto(Waiting) using Queue(q, retries - 1)
    case Event(StateTimeout, Queue(q, retries)) if retries == 0 =>
      log.error("Could not find requested target {}, giving up", selection.toString())
      stop()
  }

  when(Forwarding) {
    case Event(msg: Terminated, TargetFound(ref)) =>
      log.error("Remote Actor {} disappeared, trying to find it back", ref)
      askSelectionForIdentification()
      unstashAll()
      goto(Waiting) using Queue(Vector.empty[Any], maxRetries)
    case Event(msg, TargetFound(ref)) =>
      ref ! msg
      stay()
  }

  initialize()

  private[this] def askSelectionForIdentification(): Unit = {
    log.debug("Asking target selection {} to identify itself; messageId={}", selection.toString(), messageId)
    selection ! Identify(messageId)
  }
}
