name := "akka-streams-study"

version := "1.0-SNAPSHOT"

scalaVersion := "2.12.8"

val akkaVersion = "2.5.21"

scalacOptions in Scope.Global := Seq("-deprecation", "-feature")

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion
  , "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
  , "org.reactivestreams" % "reactive-streams" % "1.0.1"
  , "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.10.0"
  , "org.apache.logging.log4j" % "log4j-api" % "2.10.0"
  , "org.apache.logging.log4j" % "log4j-core" % "2.10.0"
  , "junit" % "junit" % "4.12" % "test"
  , "org.scalatest" %% "scalatest" % "3.0.4" % "test"
  , "org.assertj" % "assertj-core" % "3.5.2" % "test"
  , "com.novocode" % "junit-interface" % "0.11" % "test"
)
