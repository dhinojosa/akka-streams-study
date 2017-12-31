package com.xyzcorp.akka.streams

import akka.NotUsed
import akka.actor.{ActorSystem, PoisonPill}
import akka.stream.{ActorMaterializer, KillSwitches, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.language.postfixOps
import scala.util.{Failure, Success}

class SourceSpec extends FunSuite with Matchers {
  implicit val system: ActorSystem = ActorSystem("MyActorSystem")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  import scala.concurrent.duration._

  test(
    """Case 1: A Source that accepts a single item, the first element [Int]
      | is the type of element that this source emits, the second one
      | is some auxiliary value. When no auxiliary value is produced
      | akka.NotUsed is used in its place.  In order to activate it we
      | require an engine, this is called the Materializer, which
      | will connect the stream to the actors to process the data.""") {
    val single: Source[Int, NotUsed] = Source.single(3)
    single.runForeach(println)(materializer)
  }

  test("Case 2: This is the same as the previous example, with the exception " +
    "that we will use a Materializer that is implicitly bound" +
    "so as not to litter our code with explicit bindings.") {
    val single: Source[Int, NotUsed] = Source.single(3)
    single.runForeach(println)
  }

  test(
    "Case 3: A stream can begin with any Source, for one we can use a range," +
      "by using apply which takes an Iterable") {
    val range: Source[Int, NotUsed] = Source(1 to 100)
    range.runForeach(println)
  }

  test(
    """Case 4: A stream can begin with any Source, for one we can use an
           unfold, which will aggregate running state, and would have to
           return an Option deciding when done. This is a fibonacci sequence.

           In the following we start with:
               (x, y) => Some((y -> (x + y)), x)
               ---------------------------------
               (0, 1) => Some((1, 1), 0)  //0
               (1, 1) => Some((1, 2), 1)  //1
               (1, 2) => Some((2, 3), 1)  //1
               (2, 3) => Some((3, 5), 2)  //2
               (3, 5) => Some((5, 8), 3)  //3
               (5, 8) => Some((8, 13), 5) //5""") {

    val result = Source.unfold(0 → 1) {
      case (x, _) if x > 5 ⇒ None
      case (x, y) ⇒ Some((y → (x + y)) → x)
    }.toMat(Sink.collection[Int, List[Int]])(Keep.right)

    val future = result.run()
    Await.ready(future, 3 seconds)
    future.foreach(_ should be(List(0, 1, 1, 2, 3, 5)))
  }

  test(
    """Case 5: Stream is an iterable, therefore Stream.from(0) will start
               from the beginning, and continue from there. Since Stream
               is already an iterable it can just be used
               with Source.apply""") {
    val future = Source(Stream.from(0)).take(4).toMat(Sink.collection[Int,
      List[Int]])(Keep.right).run()
    Await.ready(future, 3 seconds).onComplete {
      case Success(lst) => lst should be(List(0, 1, 2, 3))
      case Failure(t) => fail(t.getMessage)
    }
  }

  test(
    """Case 6: Stream is uses an actorRef that refers to an active Actor that
      | is fed from the
      | outside using materialization manipulation""".stripMargin) {
    val value = Source
      .actorRef(2000, OverflowStrategy.dropNew)
      .viaMat(KillSwitches.single)(Keep.both)
      .toMat(Sink.collection[String, List[String]])(Keep.both)

    val tuple = value.run()
    val actorRef = tuple._1._1
    val killSwitch = tuple._1._2
    val future = tuple._2

    actorRef ! "Hello1"
    actorRef ! "Hello2"
    actorRef ! "Hello3"
    actorRef ! "Hello4"

    Thread.sleep(1000) //Keep some time let it flow

    killSwitch.shutdown() //Kill it off

    Await.ready(future, 10 seconds).onComplete {
      case Success(lst) => lst should be (List("Hello1", "Hello2",
                                              "Hello3", "Hello4"))
      case Failure(t) => fail(s"Failed with message ${t.getMessage}")
    }
  }
}
