package com.xyzcorp.akka.streams

import java.nio.file.Paths
import java.time.{LocalDateTime, ZonedDateTime}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong}
import java.util.concurrent.{CompletableFuture, TimeUnit}

import akka.actor.{ActorSystem, Cancellable}
import akka.event.{Logging, LoggingAdapter}
import akka.stream._
import akka.stream.scaladsl.{Broadcast, FileIO, Flow, Keep, RunnableGraph, Sink, Source, SourceQueueWithComplete}
import akka.util.ByteString
import akka.{Done, NotUsed}
import org.reactivestreams.{Publisher, Subscriber, Subscription}
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.{Await, ExecutionContextExecutor, Future, Promise}
import scala.language.postfixOps
import scala.util.{Failure, Success}

class SimpleStreamSpec extends FunSuite with Matchers {

  implicit val system: ActorSystem = ActorSystem("MyActorSystem")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  implicit val loggingAdapter: LoggingAdapter = Logging(system, "SimpleStreamSpec")

  val userHome: String = System.getProperty("user.home")

  test("Case 1: A Source that accepts a single item, the first element [Int] " +
    "is the type of element that this source emits, the second one " +
    "is some auxiliary value. When no auxiliary value is produced " +
    "akka.NotUsed is used in its place.  In order to activate it we" +
    "require an engine, this is called the Materializer, which " +
    "will connect the stream to the actors to process the data.") {
    val single: Source[Int, NotUsed] = Source.single(3)
    single.runForeach(println)(materializer)
  }

  test("Case 2: This is the same as the previous example, with the exception " +
    "that we will use a Materializer that is implicitly bound" +
    "so as not to litter our code with explicit bindings.") {
    val single: Source[Int, NotUsed] = Source.single(3)
    single.runForeach(println)
  }

  test("Case 3: A stream can begin with any Source, for one we can use a range," +
    "by using apply which takes an Iterable") {
    val range: Source[Int, NotUsed] = Source(1 to 100)
    range.runForeach(println)
  }

  test("Case 4: A stream can be created by independent components") {
    pending
  }

  test("Case 5: A Source can be composited to create another source by integrating a Flow") {
    val mapIntFlow: Flow[Int, Int, NotUsed] = Flow[Int].map(x => 10 + x)
    val printlnSink = Sink.foreach[Int](println)
    val compositeSource = Source(1 to 10).via(mapIntFlow)
    val future = compositeSource.runForeach(println)
    Thread.sleep(1000)
  }

  test("Case 6: A stream can be composited with Sink") {
    val mapIntFlow: Flow[Int, Int, NotUsed] = Flow[Int].map(x => 10 + x)
    val compositeSink: Sink[Int, NotUsed] = mapIntFlow.to(Sink.foreach(println))
  }

  test("Case 7: A stream can begin with any Source, for one we can use a future, " +
    "by calling from Future with a Future[+T]") {
    val future = Future {
      Thread.sleep(4000)
      40 + 10
    }
    val futureSource: Source[Int, NotUsed] = Source.fromFuture(future)
    val futureDone: Future[Done] = futureSource.runForeach(println)
    futureDone.foreach(x => println(x))
  }

  test("Case 8: Backpressure occurs when the source, or an upstream component " +
    "on another thread creates data faster than can be consumed") {
    val result: RunnableGraph[Cancellable] = Source.tick(
      100 milliseconds,
      100 milliseconds,
      "Ping")
      .async
      .map { s => Thread.sleep(1000); s }
      .to(Sink.foreach(println))
    result.run()
    Thread.sleep(5000)
  }

  test("Case 9: A stream can begin with any Source, for one we can use a " +
    "CompletableStage from the java.util.concurrent package. Remember" +
    "the following is from a Java API and not a Scala API") {

    val completableStage = CompletableFuture.supplyAsync(() => {
      Thread.sleep(4000)
      505 + 10
    })

    val completableStageSource = Source.fromCompletionStage(completableStage)
    completableStageSource.runForeach(println)
  }

  def currentThreadName: String = {
    Thread.currentThread().getName
  }

  test("Case 10: A stream can begin with any Source, for one we can use the " +
    "ReactiveStreams API Publisher\n which derives from " +
    "http://reactive-streams.org/ and we can manually emit the " +
    "items that\n we wish.") {

    val publisher = new Publisher[Long] {
      override def subscribe(s: Subscriber[_ >: Long]): Unit = {
        val done = new AtomicBoolean(false)
        val lastCount = new AtomicLong(0)

        s.onSubscribe(new Subscription {
          override def cancel(): Unit = done.set(true)

          override def request(n: Long): Unit = giveMore(n)
        })

        def giveMore(amt: Long): Unit = {
          println("Currently requesting %d on Thread: %s\n".format(amt, currentThreadName))
          val previous: Long = lastCount.getAndAdd(amt)
          println("Iterating from %d to %d on Thread: %s\n".format(previous, lastCount.get - 1, currentThreadName))
          for (i <- previous to (lastCount.get - 1)) {
            s.onNext(i)
          }
        }
      }
    }

    Source.fromPublisher(publisher).limit(30).runForeach(x => {
      Thread.sleep(5)
      println("Println %d on Thread %s".format(x, currentThreadName))
    })

    Thread.sleep(5000)
  }

  test("Case 11: A maybe source, returns a Promise[Option[Int]] which will receive a promise as its auxiliary value") {
    val maybeSource: Source[Int, Promise[Option[Int]]] = Source.maybe[Int]
    val result: RunnableGraph[Promise[Option[Int]]] =
         maybeSource.map(x => x + 1).toMat(Sink.foreach(println))(Keep.left)
    val promise: Promise[Option[Int]] = result.run()
    Thread.sleep(1000)
    promise.success(Some(100))
    Thread.sleep(1000)
  }

  test("Case 12: A RunnableGraph is a graph will all the elements connected") {
    import scala.concurrent.duration._

    val source: Source[Int, NotUsed] = Source(1 to 5) //Uses apply
    val sink: Sink[Int, Future[Int]] =
      Sink.fold[Int, Int](0)((total, next) => total + next)

    //The same as below: val runnable = source.toMat(sink)((nu, mat2) => mat2)
    val runnable = source.toMat(sink)(Keep.right)
    val i: Int = Await.result(runnable.run(), 1 seconds)
    println(i)
  }

  test("Case 13: A stream can begin with any Source, for one we can use repeat that" +
    "would continually repeat an invocation of a Function0. To keep" +
    "from this going on forever, we will use our first operator," +
    "limit which will limit the invocation to the first five times") {
    val source: Source[Int, NotUsed] = Source.repeat(10).limit(5)
    source.runForeach(println) //Prints 6 10s
    source.map(x => x + 1).runForeach(println) //Prints 5 11s
    Thread.sleep(1000)
  }

  test("Case 14: A Flow is just an interconnecting piece that can be reused") {
    val flow: Flow[Int, Int, NotUsed] = Flow.fromFunction((x: Int) => x + 1)
    val future = Future {
      Thread.sleep(10000)
      1000
    }
    Source.fromFuture(future).via(flow).runForeach(println)
    Thread.sleep(2000)
  }

  test(
    """Case 15: Using an actual Source, Flow, and Sink as separate components. Given the shapes:
      |  Sink is of type : SinkShape[In]
      |  Flow is of type : FlowShape[In, Out]
      |  Source is of type : Source[Out]
    """) {
    val intSink: Sink[Int, Future[Done]] = Sink.foreach(x => println(x))
    val onNextDo: Flow[Int, Int, NotUsed] = Flow.fromFunction(x => {
      println(x)
      x
    })

    val source2: Source[Int, NotUsed] = Source.cycle[Int](() => List(1, 2, 3, 4).iterator)
    source2.via(onNextDo).runWith(intSink)
  }

  test("""Case 16: Using FileIO as a Sink to write to a file""") {
    val flow: Flow[Int, ByteString, NotUsed] =
      Flow[Int].map(x => ByteString(x + "\n"))
    val sink: Sink[ByteString, Future[IOResult]] =
      FileIO.toPath(Paths.get(s"$userHome/akka-stream-awesome.txt"))
    val matSink: ((NotUsed, Future[IOResult]) => Nothing) => Sink[Int, Nothing] =
      flow.toMat(sink) //Connecting to the end
    val matSinkKeepRight: Sink[Int, Future[IOResult]] = flow.toMat(sink)(Keep.right)

    Source.single(4).runWith(matSinkKeepRight)
    Source.single(4).runWith(matSinkKeepRight)
  }

  test("""Case 17: Running factorials 1 to 100 to akka-stream-factorials.txt""") {
    val fact: Source[BigInt, NotUsed] =
      Source(1 to 100).scan(BigInt(1))((acc, next) => acc * next)
    fact.map(x => ByteString(s"num: $x\n"))
      .runWith(FileIO.toPath(Paths.get(s"$userHome/akka-stream-factorials.txt")))
  }

  test("""Case 18: Tick of data every second, taking the first 100 and outputting it""") {
    val source: Source[LocalDateTime, Cancellable] = Source.tick(FiniteDuration(0, TimeUnit.SECONDS),
      FiniteDuration(10, TimeUnit.MILLISECONDS), LocalDateTime.now).take(100)
    val fileSink: Sink[ByteString, Future[IOResult]] =
      FileIO.toPath(Paths.get(s"$userHome/time-output-akka-stream.txt"))
    val result: RunnableGraph[Future[IOResult]] =
      source.map(ldt => ByteString(ldt.getMinute.toString)).toMat(fileSink)(Keep.right)

    result.run().onComplete { t =>
      val str = t match {
        case Success(x) => "Successful run: " + x
        case Failure(th) => "Failure run: " + th
      }
      println(str)
    }
    Thread.sleep(5000)
  }

  test("Case 19: Perform a test with an async boundary which will run on a separate actor and dispatcher") {
    import scala.language.postfixOps
    val value = Source(1 to 10)
      .map(x => x + 1)
      .async //--from this point on, on another thread
      .filter(x => x % 2 == 0)
    val value2 = value.toMat(Sink.foreach(println))(Keep.left)
    value2.run()
    Thread.sleep(1000)
  }

  test(
    """Case 20: Perform a stream with a recover, the recover will take a
       Partial Function, and continue with another value""") {
    Source(10 to 0 by -1)
      .async
      .map(x => Some(100 / x))
      .recover { case t: Throwable => None }
      .runForeach(println)
    Thread.sleep(5000)
  }

  test("""Case 21: Perform a stream with a recoverWithRetries
      |  which will run the test a specified
      | number of times and will bail with another stream""") {
    Source(5 to 0 by -1)
      .async
      .map{x => Some(100 / x)}
      .recoverWithRetries(3, {
        case t: Throwable => Source(3 to 0 by -1).map(y => Some(100/y))})
      .runForeach(println)
    Thread.sleep(5000)
  }

  test("Case 22: Broadcast can take one input and create many outputs") {
    val asyncSource: Source[ByteString, NotUsed] = Source(1 to 10)
      .map(x => s"$x\n")
      .map(s => ByteString(s)).async

    val broadcast: Int => Broadcast[ByteString] = x => Broadcast[ByteString](x)

    val file1: Sink[ByteString, Future[IOResult]] = FileIO.toPath(Paths.get(s"$userHome/akka-stream-path1.txt"))

    val file2: Sink[ByteString, Future[IOResult]] = FileIO.toPath(Paths.get(s"$userHome/akka-stream-path2.txt"))

    val finalSink: Sink[ByteString, NotUsed] = Sink.combine(file1, file2)(broadcast)

    asyncSource.toMat(finalSink)(Keep.left)

    Thread.sleep(1000)
  }

  test("Case 23: Creation with lazily") {
    val zonedDateTimeNowSource: Source[ZonedDateTime, Future[NotUsed]] =
      Source.lazily(() => Source.single(ZonedDateTime.now))
    zonedDateTimeNowSource.runForeach(println)
    Thread.sleep(5000)
    zonedDateTimeNowSource.runForeach(println)
  }

  test("Case 24: Manual Creation with a Queue") {
    val source: Source[Int, SourceQueueWithComplete[Int]] = Source.queue[Int](10, OverflowStrategy.backpressure)
    val graph: RunnableGraph[SourceQueueWithComplete[Int]] = source.toMat(Sink.foreach(println))(Keep.left)
    val queue = graph.run()

    queue.offer(10)
    queue.offer(30)
    queue.offer(40)
    Thread.sleep(30)
    queue.offer(50)
    queue.offer(60)
    queue.offer(90)
  }

  test("Case 25: Simple logging with Attributes") {
    Source(1 to 100)
      .log("1 to 100")
      .withAttributes(Attributes.logLevels(onElement = Logging.DebugLevel))
      .runForeach(println)
  }


}
