name := "akka-streams-study"

version := "1.0-SNAPSHOT"

scalaVersion := "2.12.3"

libraryDependencies ++= Seq("com.typesafe.akka" %% "akka-stream" % "2.5.6",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "junit" % "junit" % "4.12" % "test",
  "org.assertj" % "assertj-core" % "3.5.2" % "test",
  "com.typesafe.akka" % "akka-slf4j_2.12" % "2.5.6",
  "org.slf4j" % "slf4j-simple" % "1.7.25",
  "com.novocode" % "junit-interface" % "0.11" % "test")

crossPaths := false

EclipseKeys.withSource := true

EclipseKeys.withJavadoc := true

EclipseKeys.executionEnvironment := Some(EclipseExecutionEnvironment.JavaSE18)

