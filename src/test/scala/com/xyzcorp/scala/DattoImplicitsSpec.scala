package com.xyzcorp.scala

import org.scalatest.{FunSuite, Matchers}

class DattoImplicitsSpec extends FunSuite with Matchers {

  case class MaxFactor(value:Int)
  implicit val num: MaxFactor = MaxFactor(10)

  test("Learning Implicits") {
     def foo(x:Int)(implicit y:MaxFactor) = x + y.value
     foo(10)(MaxFactor(40)) should be (50)
  }

  test("Implicitly") {
    val mf = implicitly[MaxFactor]
    mf.value should be (10)
  }

  class IntWrapper(x:Int) {
    def isOdd: Boolean = x % 2 != 0
  }

  implicit def int2IntWrapper(a:Int) = new IntWrapper(a)

  test("Test Int Wrapper") {
    10.isOdd should be(false)
  }

  test("Implicit Conversion") {
    10 + BigInt(40) should be (BigInt(50))
  }
}
