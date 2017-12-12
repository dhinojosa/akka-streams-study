package com.xyzcorp.scala


import java.util.concurrent.Executors

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.ExecutionContext

class DattoAkkaStreamSpec extends FunSuite with Matchers {

  implicit val system = ActorSystem("My-Actor-System")
  implicit val materializer = ActorMaterializer() //pump
  //1. import scala.concurrent.ExecutionContext.Implicits.global
  //2. implicit val executionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))
  implicit val executionContext = system.dispatcher

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

  test("Sample Stream with reusable Source, and a reusable map") {
    val source = Source(1 to 100)
    val log = Flow[Int].map{ x => println(s"${Thread.currentThread().getName} $x"); x }
    val sink = Sink.ignore
//
//    val oldGraph = source
//      .via(log)
//      .async
//      .via(log)
//      .async
//      .to(sink)
//
    val graph = source
      .via(log)
      .async
      .via(log)
      .async
      //.to(sink)
      //.toMat(sink)((_, right) => right)
      .toMat(sink)(Keep.right)
    val future = graph.run()
    future.foreach(d => println(d.getClass.getName))
  }
}
