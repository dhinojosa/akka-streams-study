package com.xyzcorp.akka.streams

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.{Keep, RunnableGraph, Sink, Source}
import akka.{Done, NotUsed}
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}

class KillSwitchSpec extends FunSuite with Matchers {
  implicit val system: ActorSystem = ActorSystem("MyActorSystem")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher

  private val exception: RuntimeException = new RuntimeException("Something bad happened!")

  test("Case 1: A Unique Kill Switch can control the flow of a stream and is unique to a single stream, in this case" +
    "we will gracefully shut it down") {
    val source = Source(Stream.from(1)).delay(1 second)
    val sink = Sink.foreach(println)

    val sourceWithSwitch =
      source.viaMat(KillSwitches.single)(Keep.right)

    val completeRunnable=
      sourceWithSwitch.toMat(sink)(Keep.both)

    val completion = completeRunnable.run()

    val uniqueKillSwitch = completion._1

    val doneFuture = completion._2

    doneFuture.onComplete {
      case Success(v) => println(s"Successful Run! $v")
      case Failure(thr) => println(s"Failure, Throwable is ${thr.getMessage}")
    }

    Thread.sleep(4000) //Let it sleep for a while

    println("Killing it off now!")
    uniqueKillSwitch.shutdown() //Now we turn it off safely
    println("We shut it down!")

    Thread.sleep(4000)
  }

  test("Case 2: A Unique Kill Switch can control the flow of a stream and is unique to a single stream " +
    "in this case we can kill it with an error.") {

    val source = Source(Stream.from(1)).delay(1 second)
    val sink = Sink.foreach(println)

    val sourceWithSwitch = source.viaMat(KillSwitches.single)(Keep.right)

    val completeRunnable = sourceWithSwitch.toMat(sink)(Keep.both)

    val completion = completeRunnable.run()

    val uniqueKillSwitch = completion._1

    val doneFuture = completion._2

    doneFuture.onComplete {
      case Success(v) => println(s"Successful Run! $v")
      case Failure(thr) => println(s"Failure, Throwable is ${thr.getMessage}")
    }

    Thread.sleep(4000) //Let it sleep for a while

    println("Killing it off now!")
    uniqueKillSwitch.abort(exception)
    println("We shut it down!")

    Thread.sleep(4000)
  }

  test("Case 3: Before Materialization we can create a shared kill switch, in this case we will be using it to kill " +
    "off the stream safely with a shutdown") {
    val numberSource = Source(Stream.from(1)).delay(1 second)
    val printlnSink = Sink.foreach(println)
    val alphaSource = Source.cycle(() => ('a' to 'z').toIterator).delay(1 second)

    val killSwitch = KillSwitches.shared("shared-switch")

    numberSource.via(killSwitch.flow).to(printlnSink).run()
    alphaSource.via(killSwitch.flow).to(printlnSink).run()

    Thread.sleep(4000) //Let roll

    println("Killing off both now!")
    killSwitch.shutdown()
    println("We shut it down!")

    Thread.sleep(4000)
  }

  test("Case 4: Before Materialization we can create a shared kill switch, in this case we will be using it to kill "
       + "off a stream with an error") {
    val numberSource = Source(Stream.from(1)).delay(1 second)
    val printlnSink = Sink.foreach(println)
    val alphaSource = Source.cycle(() => ('a' to 'z').toIterator).delay(1 second)

    val killSwitch = KillSwitches.shared("shared-switch")

    val value1: RunnableGraph[(NotUsed, Future[Done])] = numberSource
      .via(killSwitch.flow)
      .toMat(printlnSink)(Keep.both)

    value1.run()
    alphaSource.via(killSwitch.flow).to(printlnSink).run()

    Thread.sleep(4000) //Let's roll

    println("Killing off both now!")
    killSwitch.abort(exception)
    println("We shut it down!")

    Thread.sleep(4000)
  }
}
