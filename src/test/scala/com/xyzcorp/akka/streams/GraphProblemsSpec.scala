package com.xyzcorp.akka.streams

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, OverflowStrategy}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, MergePreferred, RunnableGraph, Sink, Source}
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.ExecutionContextExecutor

class GraphProblemsSpec extends FunSuite with Matchers {

  implicit val system: ActorSystem = ActorSystem("MyActorSystem")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  /**
    * The following example shows what a deadlock is.
    * 1. Source starts
    * 2. Merge takes the 1
    * 3. Flow prints and continues
    * 4. Broadcast takes the 1,
    * 5. Ignore takes one from the split
    * 6. The other is fed back into the merge
    */
  test("Case 1: Deadlock issues, The following shows a deadlock. ") {
    val graph = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
      import akka.stream.scaladsl.GraphDSL.Implicits._

      val source = Source.cycle(() => (1 to 100).toIterator)
      val merge = b.add(Merge[Int](2))
      val bcast = b.add(Broadcast[Int](2))

      source ~> merge ~> Flow[Int].map {s => println("Top Flow: " + s); s }     ~> bcast ~> Sink.foreach[Int](i => println("Sink:" + i))
                merge <~ Flow[Int].map {s => println("Bottom Flow: " + s); s}   <~ bcast
      ClosedShape
    })

    graph.run()
    Thread.sleep(10000)
  }

  test("Case 2: Deadlock issues avoided with merge preferred. ") {
    val graph = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
      import akka.stream.scaladsl.GraphDSL.Implicits._

      val source = Source.cycle(() => (1 to 100).toIterator)
      val merge = b.add(MergePreferred[Int](1))
      val bcast = b.add(Broadcast[Int](2))

      source ~> merge ~> Flow[Int].map {s => println("Top Flow: " + s); s }   ~> bcast ~> Sink.foreach[Int](i => println("Sink:" + i))
      merge.preferred <~ Flow[Int].map {s => println("Bottom Flow: " + s); s} <~ Flow[Int].buffer(10, OverflowStrategy.dropHead)  <~ bcast

      ClosedShape
    })

    graph.run()
    Thread.sleep(10000)
  }
}
