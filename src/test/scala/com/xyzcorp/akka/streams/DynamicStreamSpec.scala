package com.xyzcorp.akka.streams

import java.time.ZonedDateTime

import akka.{Done, NotUsed}
import akka.actor.{ActorSystem, Cancellable}
import akka.stream.{ActorMaterializer, KillSwitches}
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, MergeHub, PartitionHub, RunnableGraph, Sink, Source}
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration._
import scala.language.postfixOps

class DynamicStreamSpec extends FunSuite with Matchers {
  implicit val system: ActorSystem = ActorSystem("MyActorSystem")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  test("Case 1: MergeHub is a fan-in junction that allows any number of producers (sources). " +
    "All producers are served in a first-come, first-served basis, if the consumer cannot keep " +
    "up then all producers are backpressured") {

    val sink: Sink[String, Future[Done]] = Sink.foreach[String](println)
    val threadLabelFlow: Flow[String, String, NotUsed] = Flow[String].map(s => "(Thread: %s) %s".format(Thread.currentThread().getName, s))
    val dateTimeLabelFlow: Flow[String, String, NotUsed] = Flow[String].map(s => "[%s] %s".format(ZonedDateTime.now, s))

    val joinedFlow = threadLabelFlow.via(dateTimeLabelFlow)

    val runnableGraph: RunnableGraph[Sink[String, NotUsed]] = MergeHub
      .source[String](perProducerBufferSize = 16)
      .via(joinedFlow)
      .to(sink)

    val output: Sink[String, NotUsed] = runnableGraph.run()

    //At this point we do have a runnable graph and we can connect any elements

    Source('a' to 'z').via(Flow.fromFunction(_.toString)).runWith(output)
    Source(1 to 90).via(Flow.fromFunction(_.toString)).runWith(output)

    Thread.sleep(3000)
  }


  test("Case 2: BroadcastHub is a fan-out junction that allows any number of consumers (sinks). " +
    "The rate of the producer will automatically be timed to the speed of the slowest consumer " +
    "Consumers will attach after the sink has been materialized") {

    val sourceStatus: Source[String, Cancellable] = Source.tick(500 milliseconds, 500 milliseconds, "")

    val threadLabelFlow: Flow[String, String, NotUsed] = Flow[String]
      .map(s => "(Thread: %s) %s".format(Thread.currentThread().getName, s))

    val dateTimeLabelFlow: Flow[String, String, NotUsed] = Flow[String]
      .map(s => "[%s] %s".format(ZonedDateTime.now, s))

    val broadcastHubSink: Sink[String, Source[String, NotUsed]] = BroadcastHub.sink[String](bufferSize = 16)

    val runnableGraph: RunnableGraph[Source[String, NotUsed]] = sourceStatus
      .via(threadLabelFlow)
      .via(dateTimeLabelFlow)
      .toMat(broadcastHubSink)(Keep.right)

    val dynamicSource = runnableGraph.run() //Must be running!

    dynamicSource.runForeach(s => println("consumer 1:" + s))
    dynamicSource.runForeach(s => println("consumer 2:" + s))

    Thread.sleep(30000)
  }

  test("Case 3: Partition Hub with a stateful sink") {
    pending
  }

  test("Case 4: Partition Hub with a stateful sink") {
    pending
//    PartitionHub.statefulSink[Int](partitioner = () => (ci: PartitionHub.ConsumerInfo, Int) => {
//      ci.consumerIdByIdx()
//      199L
//    }, 2, 156)
  }
}
