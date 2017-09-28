package com.xyzcorp.akka.streams

import java.nio.file.{Files, Path, Paths}
import java.time.ZonedDateTime

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Broadcast, FileIO, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source}
import akka.stream._
import akka.util.ByteString
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.{ExecutionContextExecutor, Future}

class GraphStreamSpec extends FunSuite with Matchers {

  implicit val system: ActorSystem = ActorSystem("MyActorSystem")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  test("""DSL graphs show the flow of data and can be run within a Builder""".stripMargin) {

    val source = Source(1 to 100)
    val sink = Sink.foreach[Int](println)

    /**
      * GraphDSL is mutable: Once the GraphDSL has been constructed though, the GraphDSL
      * instance is immutable, thread-safe, and freely shareable
      */
    var runnableGraph: RunnableGraph[NotUsed] = RunnableGraph.fromGraph(GraphDSL.create() {

      //The Builder inside though is mutable, once constructed that no longer becomes an issue
      implicit builder =>

        /**
          * brings into scope the ~> operator (read as “edge”, “via” or “to”)
          * and its inverted counterpart <~ (for noting down flows in the opposite direction where appropriate).
          */
        import GraphDSL.Implicits._
        source ~> sink
        ClosedShape //Closed Shape means that the circuit is closed and is therefore runnable
    })

    runnableGraph.run() //Since the above was a closed shape, it is a runnable graph.
  }


  test(
    """DSL graphs show the flow of data and can be run within a Builder and use injection into the graph""".stripMargin) {

    val source = Source(1 to 100)
    val sink = Sink.foreach[Int](println)

    /**
      * GraphDSL is mutable: Once the GraphDSL has been constructed though, the GraphDSL
      * instance is immutable, thread-safe, and freely shareable, this call will inject the source and sink in the
      * GraphDSL
      */
    val runnableGraph: RunnableGraph[(NotUsed, Future[Done])] = RunnableGraph.fromGraph(
      GraphDSL.create(source, sink)((_, _)) { implicit builder =>
        (src, snk) =>

          /**
            * brings into scope the ~> operator (read as “edge”, “via” or “to”)
            * and its inverted counterpart <~ (for noting down flows in the opposite direction where appropriate).
            */
          import GraphDSL.Implicits._
          source ~> snk
          ClosedShape
      })

    runnableGraph.run()
  }

  test("""DSL graphs show the flow of data Broadcast and Merge using the Scala DSL""".stripMargin) {
    val source = Source(1 to 100)
    val sink = Sink.foreach[String](println)

    val runnableGraph = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      /**
        * builder.add will make a copy of the _blueprint_ that is added to it and return the inlets and
        * outlets that can be wired up, this will also ignore the Materialized value.
        */
      val broadcast: UniformFanOutShape[Int, Int] = builder.add(Broadcast[Int](2))
      val merge: UniformFanInShape[String, String] = builder.add(Merge[String](2))

      val flow_2 = Flow[Int].map(x => "Flow 2: " + x * 2)
      val flow_3 = Flow[Int].map(x => "Flow 3: " + x * 3)

      source ~> broadcast ~> flow_2 ~> merge ~> sink
      broadcast ~> flow_3 ~> merge
      ClosedShape
    })

    runnableGraph.run()
    Thread.sleep(5000)
  }


  /**
    * This shows we can create a graph. Making a Graph a RunnableGraph requires all ports to be connected
    */
  test("DSL graphs show the flow of data as a sink with one input, explicitly with determining the splitter ports") {

    val source = Source(1 to 100)

    val outputToTwoFiles: Graph[SinkShape[Int], NotUsed] = GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val fileSink1 = b.add(FileIO.toPath(Paths.get("/Users/danno/complete1.txt")))
      val fileSink2 = b.add(FileIO.toPath(Paths.get("/Users/danno/complete2.txt")))
      //This is highly important, every component can only be used once!
      val int2ByteString1, int2ByteString2 = b.add(Flow[Int].map(x => ByteString(x)))
      val splitter = b.add(Broadcast[Int](2))

      splitter.out(0) ~> int2ByteString1 ~> fileSink1.in
      splitter.out(1) ~> int2ByteString2 ~> fileSink2.in

      SinkShape.apply(splitter.in)
    }

    source.runWith(outputToTwoFiles)
    Thread.sleep(5000)
  }


  test("DSL graphs show the flow of data as a sink with one input, with an implicit graph") {
    val source = Source(1 to 100)

    val outputToTwoFiles: Graph[SinkShape[Int], NotUsed] = GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val fileSink1 = b.add(FileIO.toPath(Paths.get("/Users/danno/complete1.txt")))
      val fileSink2 = b.add(FileIO.toPath(Paths.get("/Users/danno/complete2.txt")))
      val int2ByteString1, int2ByteString2 = b.add(Flow[Int].map(x => ByteString(x)))
      val splitter = b.add(Broadcast[Int](2))

      splitter ~> int2ByteString1 ~> fileSink1.in
      splitter ~> int2ByteString2 ~> fileSink2.in

      SinkShape.apply(splitter.in)
    }

    source.runWith(outputToTwoFiles)
    Thread.sleep(5000)
  }

  test("Show a feedback loop") {

  }
}
