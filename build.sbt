import sbt.Keys._

val akkaVersion = "2.4.16"

val dependencies = Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  "org.yaml" % "snakeyaml" % "1.17",
  "com.jsuereth" %% "scala-arm" % "2.0",
  "com.github.tototoshi" %% "scala-csv" % "1.3.4",
  "org.apache.commons" % "commons-math3" % "3.6.1",
  "com.github.scopt" %% "scopt" % "3.5.0",
  "org.apache.commons" % "commons-lang3" % "3.5",
  "ch.qos.logback" % "logback-classic" % "1.1.8",
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test, it",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test, it",
  "com.h2database" % "h2" % "1.4.193" % "test, it"
)

lazy val root = (project in file(".")).settings(
  organization := "eu.semberal",
  name := "dbstress",
  version := "1.0.0-beta3-SNAPSHOT",
  scalaVersion := "2.12.1",
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings", "-Ywarn-unused-import")
).settings(resolvers ++= Seq(
  "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"
)).configs(IntegrationTest).settings(Defaults.itSettings: _*)
  .settings(libraryDependencies ++= dependencies: _*)
  .settings(packSettings: _*).settings(packMain := Map("dbstress" -> "eu.semberal.dbstress.Main"))
  .settings(packArchiveExcludes := List("VERSION", "Makefile")
)
