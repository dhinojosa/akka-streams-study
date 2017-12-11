package com.xyzcorp.akka.actors

import akka.actor.{ActorSystem, Props}
import com.xyzcorp.akka.SimpleActorScala
import org.scalatest.{FlatSpec, FunSuite}

class SimpleActorSpec extends FunSuite {

  test("Case 1: Sending a simple message to an actor") {
    val system = ActorSystem("MySystem")
    val myActor = system.actorOf(Props[SimpleActorScala], name = "simpleActorScala")
    myActor ! "Simple Test"
    myActor ! "test"
    Thread.sleep(3000)
  }

  test("Case 2: Creating an actor using a factory"){
    val system = ActorSystem("MySystem")
    val myActor = system.actorOf(Props(new SimpleActorScala), name = "simpleActorScala")
    myActor ! "Simple Test"
    myActor ! "test"
    Thread.sleep(3000)
  }

  test("Case 3: Using an actor selection to locate an actor"){
    val system = ActorSystem("MySystem")
    system.actorOf(Props[SimpleActorScala], name = "simpleActorJava")
    val actorSelection = system.actorSelection("akka://MySystem/user/simpleActorJava")
    println("Starting location selection")
    actorSelection ! "Simple Test"
    Thread.sleep(3000)
  }

  test("Case 4: If a message does not find its way, it end up in the dead letter actor") {
    val system = ActorSystem("MySystem")
    system.actorOf(Props[SimpleActorScala], name = "simpleActorJava")
    val actorSelection = system.actorSelection("akka://MySystem/user/somethingElseIShouldn\'tBeLookingFor")
    actorSelection ! "Simple Test"
    Thread.sleep(3000)
  }
}