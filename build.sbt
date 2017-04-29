name := "akka-streams-study"

version := "1.0-SNAPSHOT"

scalaVersion := "2.12.0"

libraryDependencies ++= Seq("com.typesafe.akka" %% "akka-stream" % "2.4.12",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "junit" % "junit" % "4.12" % "test",
  "org.assertj" % "assertj-core" % "3.5.2" % "test")

