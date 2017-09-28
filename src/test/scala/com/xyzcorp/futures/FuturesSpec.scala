package com.xyzcorp.futures

import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class FuturesSpec extends FunSuite with Matchers {
  test("A basic future. Processing it using foreach") {
    val executionContext = ExecutionContext.global

    val future = Future.apply {
      Thread.sleep(1000) //Wait
      println(Thread.currentThread().getName)
      40 + 50
    }(executionContext)

    future.foreach(println)(executionContext)
    Thread.sleep(4000)
  }

  test("A basic future. Processing it using onComplete") {
    import scala.concurrent.ExecutionContext.Implicits.global

    val eventualString: Future[String] = Future {
      val num = scala.util.Random.nextInt(2)
      if (num == 1) "Awesome!" else throw new RuntimeException(s"Invalid number $num")
    }
    eventualString.onComplete { t =>
      t match {
        case Success(x) => println(s"Awesome! It's $x!")
        case Failure(th) => println(s"Failed! Exception message is ${th.getMessage}")
      }
    }
  }
}
