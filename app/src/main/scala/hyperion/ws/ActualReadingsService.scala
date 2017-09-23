package hyperion.ws

import scala.concurrent.ExecutionContext

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.OverflowStrategy.dropHead
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, Sink, Source}

import hyperion.Core
import hyperion.p1.TelegramReceived
import hyperion.rest.{HyperionConversions, HyperionJsonProtocol}

/**
  * Defines the WebSocket API for getting live updates from the metering system.
  * @param messageDistributor Ref to the Actor that distributes messages.
  * @param system Actor system
  * @param executionContext an ``ExecutionContext`` for handling ``Future``s.
  */
class ActualReadingsService(val messageDistributor: ActorRef, val system: ActorSystem)(implicit executionContext: ExecutionContext)
  extends Directives with Core with HyperionJsonProtocol with HyperionConversions {
  private implicit val materializer: Materializer = ActorMaterializer()(system)

  // Create
  // 1) an Actor that will feed everything that is sent to it into a Stream.
  // 2) a Source that will emit messages sent to that Actor
  val (actorRef, source): (ActorRef, Source[Message, NotUsed]) = Source.actorRef[TelegramReceived](1, dropHead)
    // Convert its TelegramReceived messages to TextMessage values...
    .map(msg => TextMessage(meterReadingFormat.write(msg.telegram).toString()))
    // ... and feed those into a BroadcastHub
    .toMat(BroadcastHub.sink[Message])(Keep.both).run()

  // Create an actor that registers with the Message Distributor so it will receive telegrams.
  // It will then publish those telegrams to the queue.
  system.actorOf(Props(new ActualValuesHandlerActor(actorRef, messageDistributor)), "websocket-worker")

  val route: Route = path("actual") {
    handleWebSocketMessages(Flow.fromSinkAndSource(Sink.ignore, source))
  }

  // Make sure that there always is consumption of items that are fed into the actorRef.
  source.runWith(Sink.ignore)
}