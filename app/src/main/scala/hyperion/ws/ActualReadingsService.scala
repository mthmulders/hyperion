package hyperion.ws

import scala.concurrent.ExecutionContext
import scala.collection.immutable.Seq

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.OverflowStrategy.dropHead
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, MergeHub, Source}

import hyperion.Core
import hyperion.p1.TelegramReceived
import hyperion.rest.HyperionConversions.telegramWrapper
import hyperion.rest.HyperionJsonProtocol

/**
  * Defines the WebSocket API for getting live updates from the metering system.
  * @param messageDistributor Ref to the Actor that distributes messages.
  * @param system Actor system
  * @param executionContext an ``ExecutionContext`` for handling ``Future``s.
  */
class ActualReadingsService(val messageDistributor: ActorRef, val system: ActorSystem)(implicit executionContext: ExecutionContext)
  extends Directives with Core with HyperionJsonProtocol {
  private implicit val materializer: Materializer = ActorMaterializer()(system)

  // Create a dynamic backpressured pub-sub: anything emitted to the sink will
  // be pushed to any stream that is created from the source.
  private val (sink, source) = MergeHub.source[String].toMat(BroadcastHub.sink[String])(Keep.both).run()

  // Create an Actor that will push every message it receives into the Flow.
  private val queue = Source.actorRef[TelegramReceived](Int.MaxValue, dropHead)
      .map(_.telegram)                // Extract the telegram from the Akka message
      .map(telegramWrapper)           // Convert the telegram to a Meter Reading
      .map(meterReadingFormat.write)  // Serialize the Meter Reading to JSON
      .map(_.toString())              // Write the JSON to a String
      .to(sink)
      .run()

  // Describe how to process the stream of messages that comes to us via the WebSocket.
  val flow: Flow[Message, Message, NotUsed] = Flow[Message]
    .mapConcat(_ => Seq.empty[String])         // Ignore any incoming messages.
    .via(Flow.fromSinkAndSource(sink, source)) // Create a flow that injects `source` into our stream.
    .map[Message](TextMessage(_))              // Convert String values to TextMessage

  // Create an actor that registers with the Message Distributor so it will receive telegrams.
  // It will then publish those telegrams to the queue.
  system.actorOf(Props(new ActualValuesHandlerActor(queue, messageDistributor)), "websocket-worker")

  val route: Route = path("actual") {
      handleWebSocketMessages(flow)
  }

}