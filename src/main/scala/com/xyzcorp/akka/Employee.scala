package com.xyzcorp.akka

class Employee private (val firstName:String,
                        val middleName:Option[String],
                        val lastName:String)

object Employee {
  //case class does this already
  def apply(firstName:String, lastName:String) =
    new Employee(firstName, None, lastName)

  def apply(firstName:String, middleName:String, lastName:String) =
    new Employee(firstName, Some(middleName), lastName)

  def unapply(arg: Employee): Option[(String, Option[String], String)] =
    Some((arg.firstName, arg.middleName, arg.lastName))
}

