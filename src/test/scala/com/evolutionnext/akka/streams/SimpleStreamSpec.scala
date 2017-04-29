package com.evolutionnext.akka.streams

import java.nio.file.Paths
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.{FileIO, Flow, Keep, Sink, Source}
import akka.util.ByteString
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class SimpleStreamSpec extends FunSuite with Matchers {
  implicit val system = ActorSystem("MyActorSystem")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  test("has a Source that accepts a single item") {
    val single: Source[Int, NotUsed] = Source.single(3)
    single.runForeach(println)
  }

  test("has a Source that has a repeat with a take") {
    val source: Source[Int, NotUsed] = Source.repeat(10).limit(5)
    source.runForeach(println) //Prints 6 10s
    source.map(x => x + 1).runForeach(println) //Prints 5 11s
  }

  test(
    """can also have a Flow, which
      |is just an interconnecting piece that can be reused""".stripMargin) {
    val flow: Flow[Int, Int, NotUsed] = Flow.fromFunction((x: Int) => x + 1)

    val future = Future {
      Thread.sleep(10000)
      1000
    }

    Source.fromFuture(future).via(flow).runForeach(println)
  }

  test("""Using FileIO which is classified as a Materializer""") {
    val flow: Flow[Int, ByteString, NotUsed] =
      Flow[Int].map(x => ByteString(x + "\n"))
    val sink: Sink[ByteString, Future[IOResult]] =
      FileIO.toPath(Paths.get("/Users/danno/awesome.txt"))
    val matSink: ((NotUsed, Future[IOResult]) => Nothing) => Sink[Int, Nothing] =
      flow.toMat(sink)
    val matSinkKeepRight: Sink[Int, Future[IOResult]] = flow.toMat(sink)(Keep.right)

    Source.single(4).runWith(matSinkKeepRight)
  }

  test("""Cleaned up sink example""") {
    val flow: Flow[Int, ByteString, NotUsed] =
      Flow[Int].map(x => ByteString(x + "\n"))
    Source.single(4).runWith(
      flow.toMat(FileIO.toPath(Paths.get("/Users/danno/awesome.txt")))(Keep.right))
  }

  test("""Sink Direct to the output""") {
    val fact: Source[BigInt, NotUsed] =
      Source(1 to 100).scan(BigInt(1))((acc, next) => acc * next)
    fact.map(x => ByteString(s"num: $x\n"))
      .runWith(FileIO.toPath(Paths.get("/Users/danno/factorials.txt")))
  }

  test("""Rapid Data with throttle""") {

    Source
      .tick(FiniteDuration(0, TimeUnit.SECONDS), FiniteDuration(10, TimeUnit.MILLISECONDS), LocalDateTime.now)
      .throttle()
  }
}
