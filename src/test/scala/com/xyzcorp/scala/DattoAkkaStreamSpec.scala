package com.xyzcorp.scala

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.{FunSuite, Matchers}

class DattoAkkaStreamSpec extends FunSuite with Matchers {

  implicit val system = ActorSystem("My-Actor-System")
  implicit val materializer = ActorMaterializer()

  test("Sample Stream") {
    Source(1 to 100)
      .map { x => println(s"1: ${Thread.currentThread().getName} $x"); x }
      .async
      .map { x => println(s"2: ${Thread.currentThread().getName} $x"); x }
      .async
      .runForeach(x => println(s"3: ${Thread.currentThread().getName} $x"))
  }

  test("Sample Stream with Sink") {
    val graph = Source(1 to 100)
      .map { x => println(s"1: ${Thread.currentThread().getName} $x"); x }
      .async
      .map { x => println(s"2: ${Thread.currentThread().getName} $x"); x }
      .async
      .to(Sink.ignore)

    graph.run()
  }
}
