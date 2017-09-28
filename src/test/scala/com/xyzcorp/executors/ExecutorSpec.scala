package com.xyzcorp.executors

import java.util.concurrent.Executors

import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

class ExecutorSpec extends FunSuite with Matchers{
    test("Executor creation from an existing Executor") {
      val executor: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
    }
}
