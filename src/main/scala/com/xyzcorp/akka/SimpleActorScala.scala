package com.xyzcorp.akka

import akka.actor.Actor
import akka.event.Logging

class SimpleActorScala extends Actor {
  val log = Logging(context.system, this)
  var a = 0
  def receive: PartialFunction[Any, Unit] = {
    case x:String =>
      log.info("received message: " + x + " in Simple Actor Scala")
      a = a + 2
    case y:Int =>
      log.info("received int!")
      a = a + 4
  }
}
