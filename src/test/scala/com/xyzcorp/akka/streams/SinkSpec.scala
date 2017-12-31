package com.xyzcorp.akka.streams

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.util.{Failure, Success}
import scala.concurrent.duration._
import scala.language.postfixOps

class SinkSpec extends FunSuite with Matchers {
  implicit val system: ActorSystem = ActorSystem("MyActorSystem")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  test(
    "Case 1: Sink.ignore will take every element and consume it without notification and discard all the elements")
  {
    val sinkIngore = Sink.ignore
    Source(1 to 10).to(sinkIngore)
  }

  test(
    "Case 2: Sink.head will grab the first element produced in the stream and return it as a Future")
  {
    val sinkHead = Sink.head[Int]
    val result = Source(1 to 10).toMat(sinkHead)(Keep.right)
    val future = result.run()
    future.onComplete {
      case Success(h) => println(s"Head is $h")
      case Failure(th) => println(s"Head failed with message ${th.getMessage}")
    }
  }

  test(
    """Case 3: Sink.head will grab the first element produced in the stream and return it as a Future,
      | if the future is unsuccessful it will return a failure""") {
    val sinkHead = Sink.head[Int]
    val result = Source.empty[Int].toMat(sinkHead)(Keep.right)
    val future = result.run()
    future.onComplete {
      case Success(h) => println(s"Head is $h")
      case Failure(th) => println(s"Head failed with message ${th.getMessage}")
    }
  }

  test(
    """Case 4: Sink.headOption is a fine choice to avoid any issues with failure by encoding the result
      | in an Option. In this case what if the stream is full?""") {
    val sinkHeadOption = Sink.headOption[Int]
    val result = Source(1 to 100).toMat(sinkHeadOption)(Keep.right)
    result.run().onComplete {
      case Success(Some(a1)) => println(s"Got an answer: $a1")
      case Success(None) => println("Didn't get an answer")
      case Failure(t) => println(
        s"Got an exception with the message: ${t.getMessage}")
    }
  }

  test(
    """Case 5: Sink.headOption is a fine choice to avoid any issues with failure by encoding the result
      | in an Option. In this case what if the stream is empty?""") {
    val sinkHeadOption = Sink.headOption[Int]
    val result = Source.empty.toMat(sinkHeadOption)(Keep.right)
    result.run().onComplete {
      case Success(Some(a1)) => println(s"Got an answer: $a1")
      case Success(None) => println("Didn't get an answer")
      case Failure(t) => println(
        s"Got an exception with the message: ${t.getMessage}")
    }
  }

  test(
    """Case 6: Sink.cancel will cancelled immediately upstream when
      |materialized.  This uses the Subscription immediately""") {
    val sinkCancelled = Sink.cancelled[Int]
    val source = Source(1 to 100)
    source
      .named("immediately cancelled")
      .map(x => x * 2)
      .log("Item", println)
      .toMat(sinkCancelled)(Keep.none)
  }

  test("""Case 7: Sink.seq takes all the elements and aggregates to a Seq""") {
    val future = Source(1 to 10)
      .filter(_ % 3 == 0)
      .toMat(Sink.seq)(Keep.right)
      .run()
    Await.ready(future, 3 seconds).onComplete {
      case Success(sq) => sq should be(Seq(3, 6, 9))
      case Failure(t) => fail(s"Failed with message ${t.getMessage}")
    }
  }
}
