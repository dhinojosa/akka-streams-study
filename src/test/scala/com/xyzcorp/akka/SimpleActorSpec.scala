package com.xyzcorp.akka

import akka.actor.{ActorSystem, Props}
import org.scalatest.FlatSpec

class SimpleActorSpec extends FlatSpec {

  behavior of "A simple actor"

  it should "receive our message in Scala" in {
    val system = ActorSystem("MySystem")
    val myActor = system.actorOf(Props[SimpleActorScala], name = "simpleActorScala")
    myActor ! "Simple Test"
    myActor ! "test"
    Thread.sleep(3000)
  }

  it should "receive our message in Scala using a factory" in {
    val system = ActorSystem("MySystem")
    val myActor = system.actorOf(Props(new SimpleActorScala), name = "simpleActorScala")
    myActor ! "Simple Test"
    myActor ! "test"
    Thread.sleep(3000)
  }

  it should "be at location akka://MySystem/user/simpleActorJava" in {
    val system = ActorSystem("MySystem")
    system.actorOf(Props[SimpleActorScala], name = "simpleActorJava")
    val actorSelection = system.actorSelection("akka://MySystem/user/simpleActorJava")
    println("Starting location selection")
    actorSelection ! "Simple Test"
    Thread.sleep(3000)
  }

  it should "throw send a dead letter if the actor is not found" in {
    val system = ActorSystem("MySystem")
    system.actorOf(Props[SimpleActorScala], name = "simpleActorJava")
    val actorSelection = system.actorSelection("akka://MySystem/user/somethingElseIShouldn\'tBeLookingFor")
    actorSelection ! "Simple Test"
    Thread.sleep(3000)
  }
}
