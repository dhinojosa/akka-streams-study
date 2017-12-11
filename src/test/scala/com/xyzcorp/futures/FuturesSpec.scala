package com.xyzcorp.futures

import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

class FuturesSpec extends FunSuite with Matchers {

  test("Case 1: A basic future. Processing it using foreach") {
    import ExecutionContext.Implicits.global

    val future = Future {
      Thread.sleep(1000) //Wait
      println("Inside of future: " + Thread.currentThread().getName)
      40 + 50
    }

    println("Inside of test: " + Thread.currentThread().getName)

    future.map(x => x * 1000).foreach{x =>
      println("Inside of foreach: " + Thread.currentThread().getName)
      println(x)
    }
    Thread.sleep(4000)
  }


  test("A basic future. This time with for comprehension") {
    import ExecutionContext.Implicits.global

    val future = Future {
      Thread.sleep(1000) //Wait
      println("Inside of future: " + Thread.currentThread().getName)
      40 + 50
    }

    val eventualInt: Future[String] = for (x <- future) yield "" + x * 10000
    eventualInt.foreach(println)

    Thread.sleep(4000)
  }


  test("A future with flatMap") {
    import ExecutionContext.Implicits.global

    val future = Future {
      Thread.sleep(1000) //Wait
      40 + 50
    }

    val future2 = Future {
      Thread.sleep(1000) //Wait
      200
    }

    val future3: Future[Int] = future.flatMap(x => future2.map(y => x + y))

    future3.foreach(println)

    Thread.sleep(4000)
  }

  test("A future with flatMap with comprehension") {
    import ExecutionContext.Implicits.global

    val future = Future {
      Thread.sleep(1000) //Wait
      40 + 50
    }

    val future2 = Future {
      Thread.sleep(1000) //Wait
      200
    }

    val future3: Future[Int] = for (a <- future;
                                    b <- future2) yield a * b

    future3.foreach(println)

    Thread.sleep(4000)
  }


  test("Case 2: A basic Future[T]. Processing it using onComplete and Try[T]") {
    import scala.concurrent.ExecutionContext.Implicits.global

    //Create eventual String here

    //Respond to the future using onComplete

    pending
  }
}
