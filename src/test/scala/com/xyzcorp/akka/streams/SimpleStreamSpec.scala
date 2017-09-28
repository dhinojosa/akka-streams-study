package com.xyzcorp.akka.streams

import java.nio.file.Paths
import java.time.{LocalDateTime, ZonedDateTime}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong}
import java.util.concurrent.{CompletableFuture, TimeUnit}

import akka.actor.{ActorSystem, Cancellable}
import akka.stream._
import akka.stream.scaladsl.{Broadcast, FileIO, Flow, Keep, Sink, Source}
import akka.util.ByteString
import akka.{Done, NotUsed}
import org.reactivestreams.{Publisher, Subscriber, Subscription}
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}

class SimpleStreamSpec extends FunSuite with Matchers {

  implicit val system: ActorSystem = ActorSystem("MyActorSystem")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  test(
    """A Source that accepts a single item, the first element [Int] is the type of element that
      | this source emits, the second one is some auxiliary value. When no auxiliary value is produced
      | akka.NotUsed is used in its place.  In order to activate it we require an engine,
      | this is called the Materializer, which will connect the stream to the actors to process the data."""
      .stripMargin) {
    val single: Source[Int, NotUsed] = Source.single(3)
    single.runForeach(println)(materializer)
  }

  test(
    """This is the same as the previous example, with the exception that we will use a Materializer that
      | is implicitly bound so as not to litter our code with explicit bindings.""".stripMargin) {
    val single: Source[Int, NotUsed] = Source.single(3)
    single.runForeach(println)
  }

  test(
    """A stream can begin with any Source, for one we can use a range,
      | by using apply which takes an Iterable""".stripMargin) {
    val range: Source[Int, NotUsed] = Source(1 to 100)
    range.runForeach(println)
  }

  test(
    """A stream can begin with any Source, for one we can use a future,
      | by calling from Future with a Future[+T]""".stripMargin) {
    val future = Future {
      Thread.sleep(4000)
      40 + 10
    }
    val futureSource: Source[Int, NotUsed] = Source.fromFuture(future)
    val futureDone: Future[Done] = futureSource.runForeach(println)
    futureDone.foreach(x => println(x))
  }

  test(
    """A stream can begin with any Source, for one we can use a CompletableStage from
      | the java.util.concurrent package. Remember the following is from a Java API and not
      | a Scala API""".stripMargin) {
    val completableStage = CompletableFuture.supplyAsync(() => {
      Thread.sleep(4000)
      505 + 10
    })

    val completableStageSource = Source.fromCompletionStage(completableStage)
    val doneFuture = completableStageSource.runForeach(println)
    doneFuture.onComplete(_ => system.terminate())
  }

  def currentThreadName: String = {
    Thread.currentThread().getName
  }

  test(
    """A stream can begin with any Source, for one we can use the ReactiveStreams API Publisher
      | which derives from http://reactive-streams.org/ and we can manually emit the items that
      | we wish.""".stripMargin) {

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

    val doneFuture = Source.fromPublisher(publisher).limit(30).runForeach(x => {
      Thread.sleep(5)
      println("Println %d on Thread %s".format(x, currentThreadName))
    })

    doneFuture.onComplete(_ => system.terminate())
    Thread.sleep(5000)
  }

  test("A RunnableGraph is a graph will all the elements connected") {
    import scala.concurrent.duration._

    val source: Source[Int, NotUsed] = Source(1 to 5) //Uses apply
    val sink: Sink[Int, Future[Int]] = Sink.fold[Int, Int](0)((total, next) => total + next)

    //val runnable = source.toMat(sink)((nu, mat2) => mat2)
    val runnable = source.toMat(sink)(Keep.right)
    val i: Int = Await.result(runnable.run(), 1 seconds)
    println(i)
  }

  test(
    """A stream can begin with any Source, for one we can use repeat that would continually repeat an
      | invocation of a Function0. To keep from this going on forever, we will use our first operator,
      | limit which will limit the invocation to the first five times""".stripMargin) {
    val source: Source[Int, NotUsed] = Source.repeat(10).limit(5)
    source.runForeach(println) //Prints 6 10s
    val future: Future[Done] = source.map(x => x + 1).runForeach(println) //Prints 5 11s
    future.onComplete(_ => system.terminate())
  }

  test("can also have a Flow, which is just an interconnecting piece that can be reused") {
    val flow: Flow[Int, Int, NotUsed] = Flow.fromFunction((x: Int) => x + 1)
    val future = Future {
      Thread.sleep(10000)
      1000
    }
    Source.fromFuture(future).via(flow).runForeach(println)
    Thread.sleep(2000)
  }

  test(
    """Using an actual Source, Flow, and Sink as separate components. Given the shapes:
      |  Sink is of type : SinkShape[In]
      |  Flow is of type : FlowShape[In, Out]
      |  Source is of type : Source[Out]
    """.stripMargin) {
    val intSink: Sink[Int, Future[Done]] = Sink.foreach(x => println(x))
    val onNextDo: Flow[Int, Int, NotUsed] = Flow.fromFunction(x => {
      println(x)
      x
    })

    val source2: Source[Int, NotUsed] = Source.cycle[Int](() => List(1, 2, 3, 4).iterator)
    source2.via(onNextDo).runWith(intSink)
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

  test("""Tick of data every second, taking the first 100 and outputting it""") {
    val source: Source[LocalDateTime, Cancellable] = Source.tick(FiniteDuration(0, TimeUnit.SECONDS),
      FiniteDuration(10, TimeUnit.MILLISECONDS), LocalDateTime.now).take(100)
    val fileSink: Sink[ByteString, Future[IOResult]] = FileIO.toPath(Paths.get("/home/danno/time-output-akka-stream.txt"))
    val result = source.map(ldt => ByteString(ldt.getMinute.toString)).toMat(fileSink)(Keep.right)
    result.run().onComplete { t =>
      val str = t match {
        case (Success(x)) => "Successful run: " + x
        case (Failure(th)) => "Failure run: " + th
      }
      println(str)
      system.terminate()
    }
    Thread.sleep(5000)
  }

  test("Perform a test with an async boundary which will run on a separate actor and dispatcher") {

    import scala.language.postfixOps
    Source(1 to 10)
      .map(1 +)
      .map(x => {
        println("Top: " + currentThreadName)
        x
      })
      .async
      .map(x => {
        println("Bottom " + currentThreadName)
        x
      })
      .filter(x => x % 2 == 0)
      .runForeach(println)
  }


  test("Perform a test with a recover, the recover will take a Partial Function") {
    Source(10 to 0 by -1)
      .async
      .map(x => Some(100 / x))
      .recover { case t: Throwable => None }
      .runForeach(println)
    Thread.sleep(5000)
  }

  test("Broadcast can take one input and create many outputs") {
    val asyncSource: Source[ByteString, NotUsed] = Source(1 to 10)
      .map(x => s"$x\n")
      .map(s => ByteString(s)).async

    val broadcast: Int => Broadcast[ByteString] = x => Broadcast[ByteString](x)

    val file1: Sink[ByteString, Future[IOResult]] = FileIO.toPath(Paths.get("/Users/danno/path1.txt"))
    val file2: Sink[ByteString, Future[IOResult]] = FileIO.toPath(Paths.get("/Users/danno/path2.txt"))

    val finalSink: Sink[ByteString, NotUsed] = Sink.combine(file1, file2)(broadcast)

    asyncSource.toMat(finalSink)(Keep.left)

    Thread.sleep(1000)
  }

  test("Creation with lazily") {
    val zonedDateTimeNowSource: Source[ZonedDateTime, Future[NotUsed]] =
          Source.lazily(() => Source.single(ZonedDateTime.now))
    zonedDateTimeNowSource.runForeach(println)
    Thread.sleep(5000)
    zonedDateTimeNowSource.runForeach(println)
  }
}
