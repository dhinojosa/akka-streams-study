package com.xyzcorp.implicits

import java.math.BigInteger

import org.scalatest.{FunSuite, Matchers}



class ImplicitsSpec extends FunSuite with Matchers {


  test("Manni's question of scope") {
    implicit val scope2: Int = 800
    implicit val scope3: String = "700"
    def awesome(implicit x:String, y:Int) = x + y + 2

    awesome should be ("7008002")
  }

  class MyInt(x:Int) {
      def isOdd(): Boolean = x % 2 != 0
      def isEven(): Boolean = !isOdd()
  }


  class MyInt2(x:Int) {
    def isOddLike(): Boolean = x % 2 != 0
    def isEvenLike(): Boolean = !isOddLike()
  }

  //[ key                 ,    value                   ]
  //[Function1[Int, MyInt],   (x:Int) => new MyInt(x)  ]

  test("new functionality with int") {
    implicit def foo(x:Int) = new MyInt(x)
    implicit def bar(x:Int) = new MyInt2(x)

    10.isOdd() should be (false)
  }

  test("convert items") {
    val bi = BigInt.apply(4303403)
    val bi2 = new java.math.BigInteger("49933")

    bi + bi2
  }

  test("sorting strings, pretty easy") {
    val strings = List("Eggs", "Ham", "Pinapple", "Brown Sugar")
    println(strings.sorted)
  }

  case class Employee(firstName:String, lastName:String)


  object MyPredef {
    implicit val orderingEmployeeByFirstName = new Ordering[Employee] {
      override def compare(x: Employee, y: Employee) = {
        x.firstName.compareTo(y.firstName)
      }
    }

    implicit val orderingEmployeeByLastName = new Ordering[Employee] {
      override def compare(x: Employee, y: Employee) = {
        x.lastName.compareTo(y.lastName)
      }
    }
  }

  val e = Employee.apply("Prince", "Nelson")

  test("custom employee sorting") {
    import MyPredef.orderingEmployeeByLastName
    val sorted1 = List(new Employee("Eric", "Clapton"),
      new Employee("Jeff", "Beck"),
      new Employee("Ringo", "Starr"),
      new Employee("Paul", "McCartney"),
      new Employee("John", "Lennon"),
      new Employee("George", "Harrison"),
      new Employee("Tom", "Petty")).sorted
    println(sorted1)
  }

  test("Making a future") {


  }
}
