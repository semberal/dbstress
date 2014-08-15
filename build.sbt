import sbt.Keys._

val akkaVersion = "2.3.4"
val scalamockVersion = "3.1.2"

val compileDependencies = Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
  "joda-time" % "joda-time" % "2.3",
  "org.yaml" % "snakeyaml" % "1.13",
  "com.jsuereth" %% "scala-arm" % "1.4",
  "org.scalanlp" %% "breeze" % "0.8.1",
  "com.typesafe.play" %% "play-json" % "2.3.1",
  "com.github.scopt" %% "scopt" % "3.2.0"
)

val runtimeDependencies = Seq(
  "ch.qos.logback" % "logback-classic" % "1.1.2" % "runtime"
)

val testDependencies = Seq(
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test, it",
  "org.scalatest" %% "scalatest" % "2.2.0" % "test, it",
  "org.scalamock" %% "scalamock-core" % scalamockVersion % "test, it",
  "org.scalamock" %% "scalamock-scalatest-support" % scalamockVersion % "test, it",
  "org.clapper" %% "grizzled-scala" % "1.2" % "test, it",
  "com.h2database" % "h2" % "1.4.178" % "test, it"
)

lazy val root = (project in file(".")).settings(
  organization := "eu.semberal",
  name := "dbstress",
  version := "1.0.0-beta2-SNAPSHOT",
  scalaVersion := "2.11.1",
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings", "-Xmax-classfile-name", "140")
).settings(resolvers ++= Seq(
  "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"
)).configs(IntegrationTest).settings(Defaults.itSettings: _*)
  .settings(packSettings: _*).settings(packMain := Map("dbstress" -> "eu.semberal.dbstress.Main"))
  .settings(libraryDependencies ++= (compileDependencies ++ runtimeDependencies ++ testDependencies): _*)



