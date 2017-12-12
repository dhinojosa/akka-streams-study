package com.xyzcorp.scala

import java.util.concurrent.Executors

import com.xyzcorp.akka.Employee
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}


class ScalaQuestionsTest extends FunSuite with Matchers {
  test("Test our companion object") {
    val employee = Employee("Peter", "Salu")
    employee.firstName should be("Peter")
  }


  //Outside method, not belonging to either Employee class or object
  def extractFirstName(a: Any): String = {
    val result = a match {
      case Employee(fn, Some(mn), _) => s"$fn: Middle $mn"
      case Employee(fn, _, _) => s"$fn: No middle"
      case _ => "Don't know"
    }
    result
  }

  test("Pattern match with Employee") {
    val employee = Employee("Peter", "Adewunmi", "Salu")
    val result = extractFirstName(employee)
    result should be("Peter: Middle Adewunmi")
  }


  def processFunction(x: Int => Int): Int = {
    x(100) + 500
  }

  test("currying") {
    val f = (x: Int, y: Int, z: Int) => x + y + z
    val fc = f.curried
    val fcRaw = (x: Int) =>
      (y: Int) =>
        (z: Int) => x + y + z

    val fc1: Int => Int => Int = fc.apply(3)
    val fc2: Int => Int = fc1.apply(5)
    val fc3: Int = fc2.apply(10)

    processFunction(fc2)

    fc1.apply(40)

    fc3 should be(18)

    val fc2FilledIn = fc(1)(3)
  }

  test("curriedParameters") {
    def foo(x: Int)(y: Int, z: Int)(aa: List[String]) = x + y + z

    val f = foo(3) _

    f(10, 12) should be(25)
  }


  class Foo2(x: Int) {
    def bar3(y: Int, z: Int, zz: Int) = x + y + z + zz
  }

  class Foo(x: Int) {
    def bar(y: Int) = x + y
  }

  test("converting def to function") {
    val foo = new Foo(10)

    val f = foo.bar _


    val foo2 = new Foo2(30)
    val f2 = foo2.bar3(7, _: Int, 6)

    f2(10) should be(53)
  }

  test("functions") {
    val r = List(1, 2, 3, 4).map(x => x + 1)
    r should be(List(2, 3, 4, 5))

    val foo = new Foo(10)
    val r2 = List(1, 2, 3, 4).map(x => foo.bar(x))
    val r2_1 = List(1, 2, 3, 4).map(foo.bar _)
    val r2_2 = List(1, 2, 3, 4).map(foo.bar)

    r2 should be(List(11, 12, 13, 14))

    import scala.language.postfixOps
    val r3 = List(1, 2, 3, 4).map(1 +)
  }

  //Typical functions in functional programming
  //map
  //filter
  //reduce, fold, foldLeft, reduceRight, reduceLeft (1)
  //collect
  //zip
  //flatMap

  test("foldLeft") {
    val result = List(1, 2, 3, 4).foldLeft(0) { (total, next) =>
      println(s"total: $total, next: $next")
      total + next
    }
    result should be(10)
  }

  test("foldRight") {
    val result = List(1, 2, 3, 4).foldRight(0) { (next, total) =>
      println(s"total: $total, next: $next")
      total + next
    }
    result should be(10)
  }

  test("reduceLeft") {
    val result = List[Int]().reduceLeftOption { (total, next) =>
      println(s"total: $total, next: $next")
      total + next
    }
    result should be(None)

    List(1, 2, 3).reduceLeftOption(_ + _).getOrElse(-1) should be(6)
    List(1, 2, 3).reduceLeftOption(_ + _) should be(Some(6))
    List(1, 2, 3).reduceLeftOption(_ + _).map(x => x * 6) should be(Some(36))
    List[Int]().reduceLeftOption(_ + _).map(x => x * 6) should be(None)
  }

  import scala.concurrent.duration._

  implicit val executor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))

  def timed[A](x: => A): (Long, A) = {
    val start = System.currentTimeMillis()
    val result = x
    (System.currentTimeMillis() - start, result)
  }


  test("by-name parameter") {
    val tuple = timed {
      Thread.sleep(5000)
      400
    }
    println(tuple)
  }

  test("futures") {

    val future = Future {
      Thread.sleep(5000)
      println("1:" + Thread.currentThread().getName)
      500
    }

    future.map(10 +).foreach {
      x =>
        println("2:" + Thread.currentThread().getName)
        println(x)
    }

    println("3:" + Thread.currentThread().getName)

    Thread.sleep(6000)
  }

  test("futures with flatMap") {
    val future1 = Future {
      Thread.sleep(5000)
      println("1:" + Thread.currentThread().getName)
      500
    }

    val future2 = Future {
      Thread.sleep(2000)
      println("2:" + Thread.currentThread().getName)
      10
    }

//    val result = future1.flatMap(a => future2.map(b => a + b))

    val result = for {a <- future1
                      b <- future2 } yield a + b

    Await.result(result, 8 seconds)

    result.foreach(println)

    //Thread.sleep(8000)
  }


  def parameterizeFuture(x: Int): Future[Int] =
    Future {
      Thread.sleep(5000)
      println("1:" + Thread.currentThread().getName)
      x + 500
    }


  test("future with closure 2?") {
    val f1 = parameterizeFuture(10)
    //Not idiomatic
    val result1 = f1.foreach { x =>
      val f2 = parameterizeFuture(x)
      f2.foreach(println)
    }

    val result2 = parameterizeFuture(10).flatMap(x => parameterizeFuture(x).map(y => y))
    //val result2 = for (x <- parameterizeFuture(10); y <- parameterizeFuture(x)) yield y
    //val result2 = for (x <- f1; y <- parameterizeFuture(x)) yield y

    result2.foreach(println) //Preferred

    val result = Await.result(result2, 10 seconds)  //Blocks
  }

  test("function with closure") {
    val a = 1000
    val f = (x:Int) => x + a
    f(10)
  }
  test("future with a closure") {
    parameterizeFuture(10).foreach(println)
    val future = parameterizeFuture(12)
    future.foreach(println)
    Await.result(future, 6 seconds)
  }

  def myMethod[A, B](c: Int, x : A)(f: A => B): List[B] = {
    (1 to c).map(_ => x).map(_ => f(x)).toList
  }

  test("No by name block") {
    myMethod(10, "Foo") { s =>
      val result = s.length
      result + 10
    }
  }

  test("future with onComplete") {
    val future = parameterizeFuture(100)
    Await.result(future, 6 seconds) //usually not necessary
    future.onComplete {
      case Success(x) => println(s"Success! $x")
      case Failure(e) => println(s"Super sad ${e.getMessage}")
    }
  }
}














