package com.xyzcorp.akka.streams

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.ExecutionContextExecutor

class SourceSpec extends FunSuite with Matchers {
  implicit val system: ActorSystem = ActorSystem("MyActorSystem")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  import scala.concurrent.duration._

  test(
    """Case 1: Source.apply or just Source(...) will take an Iterable. Which means, that it will take
      | anything from a List, Set, Map, Stream, and more. Here it is with a Range""") {
    Source(1 to 10).runForeach(println)
  }

  test(
    """Case 2: Source.empty or just Source(...) will take an Iterable. Which means, that it will take
      | anything from a List, Set, Map, Stream, and more. Here it is with a Range""") {
    Source.empty.runForeach(println)
    pending
  }
}
