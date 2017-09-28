package com.xyzcorp.akka.streams

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.javadsl.MergePreferred
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.stream.scaladsl.{Flow, Interleave, Keep, Merge, MergePrioritized, Source}
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.language.postfixOps
import scala.util.Random

class SourceCombinatorStreamSpec extends FunSuite with Matchers {
  implicit val system: ActorSystem = ActorSystem("MyActorSystem")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  import scala.concurrent.duration._

  test("map of course maps each element") {
    Source(1 to 10).map(x => x + 64).map(x => x.toChar + " ").runForeach(println)
  }

  test("flatMapConcat") {
    Source.single("This is a sentence")
      .flatMapConcat(w => Source(w.split("""\s""").toList))
      .runForeach(println)
  }

  test("filter") {
    Source(1 to 10).filter(_ % 2 != 0).runForeach(println)
  }

  test("filterNot") {
    Source(1 to 10).filterNot(_ % 2 != 0).runForeach(println)
  }

  test("Test with a delay") {
    Source(1 to 10).delay(10 seconds).runForeach(println)
    Thread.sleep(12000)
  }

  test("tick is an infinite source that continually emits items") {
    Source(Stream.from(0)).runForeach { i => Thread.sleep(5); println(i) }
  }

  test("Throttle sends elements downstream with speed limited to elements/per. " +
    "In other words, this stage set the maximum rate for emitting messages. " +
    "ThrottleMode.Shaping make pauses before emitting messages to meet throttle rate") {
    Source(Stream.from(0))
      .throttle(1, 1 second, 10, ThrottleMode.shaping)
      .runForeach(println)
    Thread.sleep(20000)
  }

  test("Throttle sends elements downstream with speed limited to elements/per. " +
    "In other words, this stage set the maximum rate for emitting messages. " +
    "ThrottleMode.enforcing makes throttle fail with exception when upstream is faster than throttle rate") {
    Source(Stream.from(0))
      .throttle(1, 1 second, 10, ThrottleMode.enforcing)
      .runForeach(println)
    Thread.sleep(20000)
  }

  test("recover allows you to emit a final element and then complete the stream " +
    "on an upstream failure. Deciding which exceptions should be recovered is done " +
    "through a PartialFunction. If an exception does not have a matching case the " +
    "stream is failed. This is useful if you wish to finish a stream gracefully from an error") {
    Source(Stream.from(0))
      .throttle(1, 1 second, 10, ThrottleMode.enforcing)
      .recover(new PartialFunction[Throwable, Int] {
        override def isDefinedAt(x: Throwable): Boolean = true

        override def apply(v1: Throwable): Int = -1
      })
      .runForeach(println)
    Thread.sleep(20000)
  }

  test("recover allows you to emit a final element and then complete the stream " +
    "on an upstream failure. Deciding which exceptions should be recovered is done " +
    "through a PartialFunction. If an exception does not have a matching case the " +
    "stream is failed. This is useful if you wish to finish a stream gracefully from an error. " +
    "In this case we will use an Either type") {
    Source(Stream.from(0))
      .throttle(1, 1 second, 10, ThrottleMode.enforcing)
      .map[Either[Throwable, Int]](x => Right(x))
      .recover(new PartialFunction[Throwable, Either[Throwable, Int]] {
        override def isDefinedAt(x: Throwable): Boolean = true

        override def apply(v1: Throwable): Left[Throwable, Int] = Left[Throwable, Int](v1)
      })
      .runForeach(println)
    Thread.sleep(20000)
  }

  test("recover with retries allows you to emit a final element and then complete the stream " +
    "on an upstream failure, but each time, it will retry, this is useful if you are making a " +
    "network connection and would need to give it a few attempts, backpressure is built in") {
    Source.cycle(() => 10 to 0 by -1 toIterator)
      .map(100 /)
      .recoverWithRetries(2, { case t: Throwable =>
        printf("Got Throwable: %s", t.getMessage)
        Source.empty
      })
      .runForeach(println)
    Thread.sleep(1000)
  }

  test("combine combines two sources so that they are interweaved, in this case we use merge to combine") {
    val oneToOneHundred = Source(1 to 100)
    val twoHundredToThreeHundred = Source(200 to 300)
    Source.combine(oneToOneHundred, twoHundredToThreeHundred)(i => Merge(i)).runForeach(println)
  }

  test("combine combines two sources so that they are interweaved, in this case " +
    "we use interleave that can choose, a number for the segment size") {
    val oneToOneHundred = Source(1 to 100)
    val twoHundredToThreeHundred = Source(200 to 300)
    Source.combine(oneToOneHundred, twoHundredToThreeHundred)(i => Interleave(i, 2)).runForeach(println)
  }

  test("combine combines two sources so that they are interweaved, in this case " +
    "we use merge prioritized to set a priority as to which one should be listened to first depending on weight") {
    val oneToOneHundred = Source(1 to 100)
    val twoHundredToThreeHundred = Source(200 to 300)
    Source.combine(oneToOneHundred, twoHundredToThreeHundred)(i => MergePrioritized(Seq(2, 1))).runForeach(println)
  }

  test("zip will create a stream of tuples from each of the sources") {
    Source(1 to 100).zip(Source('a' to 'z')).runForeach(println)
  }

  test("zip will create a stream of tuples from each of the sources, and will wait until another element is available") {
    Source(1 to 100)
      .zip(Source('a' to 'z')
        .throttle(1, 5 millisecond, 3, ThrottleMode.shaping))
      .runForeach(println)
  }

  test("zip with will create whatever with whatever function you would like from each of the sources, and will " +
    "wait until another element is available") {
    Source(1 to 100)
      .zipWith(Source('a' to 'z'))((n, c) => "Item:" + c + n)
      .runForeach(println)
  }

  test("zip with will create whatever with whatever function you would like from each of the sources, and will " +
    "wait until another element is available") {
    val source: Source[(Int, Char), NotUsed] = Source(1 to 100)
      .zipMat(Source('a' to 'z'))(Keep.left)
  }

  test("difference between zip and zipWithMap") {

    val firstZip: Source[(Int, Char), NotUsed] = Source(1 to 100)
      .zip(Source('a' to 'z'))

    val secondZip: Source[(Int, Char), (NotUsed, NotUsed)] = Source(1 to 100)
      .zipMat(Source('a' to 'z'))(Keep.both)

    val thirdZip = Source(1 to 100)
      .zipWith(Source('a' to 'z'))((n, c) => "Item: %d %c".format(n, c))

    val fourthZip = Source(1 to 100)
      .zipWithMat(Source('a' to 'z'))((n, c) => "Item: %d %c".format(n, c))(Keep.both)
  }


}
