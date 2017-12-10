import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.OverflowStrategy.dropHead
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, MergeHub, RunnableGraph, Sink, Source}

import scala.io.StdIn

class WebSocketBridge(client: ActorRef) extends Actor with ActorLogging {
  override def preStart = {
    log.info("Starting bridge...")
  }

  override def receive = {
    case msg: String =>
      log.debug(s"I haz msg [$msg]")
      client ! msg
  }
}

class Service(implicit materializer: Materializer, system: ActorSystem) extends Directives {
  val (actorRef, source): (ActorRef, Source[Message, NotUsed]) = Source.actorRef[String](1, dropHead)
    .map[Message](TextMessage(_))
    .toMat(BroadcastHub.sink[Message])(Keep.both).run()

  val actor = system.actorOf(Props(new WebSocketBridge(actorRef)))

  val route = path("test") {
    handleWebSocketMessages(Flow.fromSinkAndSource(Sink.ignore, source))
  }

  source.runWith(Sink.ignore)
  source.runForeach(msg => System.out.println(s"\t${msg.asTextMessage.getStrictText}"))
}

object Example extends App {
  implicit val system = ActorSystem("example")
  implicit val materializer = ActorMaterializer()

  val service = new Service()

  Http().bindAndHandle(Route.handlerFlow(service.route), "0.0.0.0", 7999)

  while (!"q".equals(StdIn.readLine())) {
    service.actor ! "Howdy!"
  }
}