package com.xyzcorp.akka.streams

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Interleave, Keep, Merge, MergePrioritized, Source}
import akka.stream.{ActorMaterializer, DelayOverflowStrategy, ThrottleMode}
import org.scalatest.{FunSuite, Matchers}

import scala.collection.immutable.{Seq => ImmutableSeq}
import scala.concurrent.ExecutionContextExecutor
import scala.language.postfixOps

class SourceCombinatorStreamSpec extends FunSuite with Matchers {
  implicit val system: ActorSystem = ActorSystem("MyActorSystem")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  import scala.concurrent.duration._

  test("Case 1: Applies a function to element in the stream") {
    Source(1 to 10).map(x => x + 64)
      .map(x => x.toChar + " ")
      .runForeach(println)
  }

  test("Case 2: filter will only pass on those elements that satisfy the given predicate") {
    Source(1 to 10).filter(_ % 2 != 0).runForeach(println)
  }

  test("Case 3: filterNot will negate what filter does") {
    Source(1 to 10).filterNot(_ % 2 != 0).runForeach(println)
  }

  test("Case 4: Delay will delay the stream by the duration presented") {
    Source(1 to 10).delay(10 seconds).runForeach(println)
    Thread.sleep(12000)
  }

  test("""Case 5: Delay will delay the stream by the duration presented, as well as receive
       an DelayOverflowStrategy, which contains, backpressure, dropBuffer, dropHead,
       dropNew, dropTail, emitEarly, and fail""") {
    Source(1 to 10).delay(10 seconds, DelayOverflowStrategy.dropBuffer).runForeach(println)
    Thread.sleep(12000)
  }

  test("""Case 6: flatMapConcat transforms each element into a `Source` of output elements
      then flattened into the output stream by concatenation,
      fully consuming one Source after the other.""") {
    Source.single("This is a sentence")
      .flatMapConcat(w => Source.apply[String](w.split("""\s""").to[ImmutableSeq]))
      .runForeach(println)
  }

  test("""Case 7: Stream is an iterable, therefore Stream.from(0) will start from the beginning,
         and continue from there. Since Stream is already an iterable it can just be used with
          Source.apply""") {
    Source(Stream.from(0)).runForeach { i => Thread.sleep(5); println(i) }
  }


  test(
    """Case 8: Throttle sends elements downstream with speed limited to elements/per
         In other words, this stage set the maximum rate for emitting messages.
         ThrottleMode.Shaping make pauses before emitting messages
         to meet throttle rate""") {
    Source(Stream.from(0))
      .throttle(1, 1 second, 10, ThrottleMode.shaping)
      .runForeach(println)
    Thread.sleep(20000)
  }

  test(
    """Case 9: Throttle sends elements downstream
          with speed limited to elements / per
          In other words, this stage set the maximum rate for emitting messages.
          ThrottleMode.enforcing makes throttle fail with exception
          when upstream is faster than throttle rate""") {
    Source(Stream.from(0))
      .throttle(1, 1 second, 10, ThrottleMode.enforcing)
      .runForeach(println)
    Thread.sleep(20000)
  }

  test("Case 10: Recover allows you to emit a final element and then complete the stream " +
    "on an upstream failure. Deciding which exceptions should be recovered is done " +
    "through a PartialFunction. If an exception does not have a matching case the " +
    "stream is failed. This is useful if you wish to finish a stream gracefully from an error") {
    Source(Stream.from(0))
      .throttle(1, 1 second, 10, ThrottleMode.enforcing)
      .recover(new PartialFunction[Throwable, Int] {
        override def isDefinedAt(x: Throwable): Boolean = true

        override def apply(v1: Throwable): Int = {
          -1
        }
      })
      .runForeach(println)
    Thread.sleep(20000)
  }

  test("Case 11: Recover allows you to emit a final element and then complete the stream " +
    "on an upstream failure. Deciding which exceptions should be recovered is done " +
    "through a PartialFunction. If an exception does not have a matching case the " +
    "stream is failed. This is useful if you wish to finish a stream gracefully from an error. " +
    "In this case we will use an Either type.") {
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

  test("Case 12: Recover with retries allows you to emit a final element and then complete the stream " +
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

  test("Case 13: combine combines two sources so that they are interweaved, in this case we use merge to combine") {
    val oneToOneHundred = Source(1 to 100)
    val twoHundredToThreeHundred = Source(200 to 300)
    Source.combine(oneToOneHundred, twoHundredToThreeHundred)(i => Merge(i)).runForeach(println)
  }

  test("Case 14: combine combines two sources so that they are interweaved, in this case " +
    "we use interleave that can choose, a number for the segment size") {
    val oneToOneHundred = Source(1 to 100)
    val twoHundredToThreeHundred = Source(200 to 300)
    Source.combine(oneToOneHundred, twoHundredToThreeHundred)(i => Interleave(i, 2)).runForeach(println)
  }

  test("Case 15: combine combines two sources so that they are interweaved, in this case " +
    "we use merge prioritized to set a priority as to which one should be listened to first depending on weight") {
    val oneToOneHundred = Source(1 to 100)
    val twoHundredToThreeHundred = Source(200 to 300)
    Source.combine(oneToOneHundred, twoHundredToThreeHundred)(i => MergePrioritized(Seq(2, 1))).runForeach(println)
  }

  test("Case 16: zip will create a stream of tuples from each of the sources") {
    Source(1 to 100).zip(Source('a' to 'z')).runForeach(println)
  }

  test("""Case 17: zip will create a stream of tuples from each of the sources
       and will wait until another element is available. Some notes:
           1. 100 is vastly larger than a through z
           2. Throttle will slow things down but not by much""") {
    Source(1 to 100)                                          //100 is vastly larger than a..z
      .zip(Source('a' to 'z')
        .throttle(1, 1 second, 3, ThrottleMode.shaping)) //Causing a wait
      .runForeach(println)
    Thread.sleep(10000)
  }

  test("""Case 18: zipWith will create whatever with whatever function
       you would like from each of the sources, and will wait until
       another element is available""") {
    Source(1 to 100)
      .zipWith(Source('a' to 'z'))((n, c) => "Item:" + c + n)
      .runForeach(println)
  }

  test("""Case 19: zipMat will allow you to choose which of the auxiliary information you
        would like to carry through""") {
    val source: Source[(Int, Char), NotUsed] = Source(1 to 100)
      .zipMat(Source('a' to 'z'))(Keep.left)
  }
}
