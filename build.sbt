name := "akka-streams-study"

version := "1.0-SNAPSHOT"

scalaVersion := "2.12.4"

val akkaVersion = "2.5.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion
  , "com.typesafe.akka" % "akka-slf4j_2.12" % akkaVersion
  , "org.reactivestreams" % "reactive-streams" % "1.0.1"
  , "org.slf4j" % "slf4j-simple" % "1.7.25"
  , "junit" % "junit" % "4.12" % "test"
  , "org.scalatest" %% "scalatest" % "3.0.4" % "test"
  , "org.assertj" % "assertj-core" % "3.5.2" % "test"
  , "com.novocode" % "junit-interface" % "0.11" % "test"
)
