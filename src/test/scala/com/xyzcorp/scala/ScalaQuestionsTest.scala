package com.xyzcorp.scala

import com.xyzcorp.akka.Employee
import org.scalatest.{FunSuite, Matchers}




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
    val r2 = List(1,2,3,4).map(x => foo.bar(x))
    val r2_1 = List(1,2,3,4).map(foo.bar _)
    val r2_2 = List(1,2,3,4).map(foo.bar)

    r2 should be (List(11, 12, 13, 14))

    import scala.language.postfixOps
    val r3 = List(1,2,3,4).map(1+)
  }

  //Typical functions in functional programming
  //map
  //filter
  //reduce, fold, foldLeft, reduceRight, reduceLeft (1)
  //collect
  //zip
  //flatMap

  test("foldLeft") {
    val result = List(1,2,3,4).foldLeft(0){(total, next) =>
      println(s"total: $total, next: $next")
      total + next}
    result should be (10)
  }

  test("foldRight") {
    val result = List(1,2,3,4).foldRight(0){(next, total) =>
      println(s"total: $total, next: $next")
      total + next}
    result should be (10)
  }

  test("reduceLeft") {
    val result = List[Int]().reduceLeftOption{(total, next) =>
      println(s"total: $total, next: $next")
      total + next}
    result should be (None)

    List(1,2,3).reduceLeftOption(_ + _).getOrElse(-1) should be (6)
    List(1,2,3).reduceLeftOption(_ + _) should be (Some(6))
    List(1,2,3).reduceLeftOption(_ + _).map(x => x * 6) should be (Some(36))
    List[Int]().reduceLeftOption(_ + _).map(x => x * 6) should be (None)
  }

  test("futures") {

    //val future = Future.apply

  }

}














