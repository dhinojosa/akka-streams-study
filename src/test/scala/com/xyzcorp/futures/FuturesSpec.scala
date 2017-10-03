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
    pending
  }
}
