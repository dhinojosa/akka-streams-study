package com.evolutionnext.akka.streams

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.stream.{ActorMaterializer, Attributes}
import org.scalatest.Matchers
import org.scalatest.FlatSpec
import akka.stream.scaladsl.{Flow, Keep, RunnableGraph, Sink, Source}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

class SampleFreeSpec extends FlatSpec with Matchers {

  implicit val system: ActorSystem = ActorSystem("MyActorSystem")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  implicit val log: LoggingAdapter = Logging(system, "custom-logger")

  "A Stream" should "be fairly simple to set up" in {
    10 should be(10)
  }

  it should "also crush the competition" in {
    val fireLog = Flow[Int].log("fire", x => s"Just reacted to a flow $x")
      .withAttributes(Attributes
        .createLogLevels(Logging.InfoLevel, Logging.InfoLevel, Logging.ErrorLevel))

    val source: Source[Int, NotUsed] = Source(1 to 10).via(fireLog)
    source.runForeach(println)
  }

  it should "has sink info" in {
    val sink: Sink[Int, Future[Option[Int]]] = Sink.lastOption[Int]

    val result: RunnableGraph[Future[Option[Int]]] = Source(1 to 100).toMat(sink)(Keep.right)

    result.run().onComplete {
      case Success(Some(x)) => println(s"Success: Returned value $x")
      case Success(None) => println(s"Success: No value")
      case Failure(t) => t.printStackTrace()
    }

    Thread.sleep(1000)
  }
}