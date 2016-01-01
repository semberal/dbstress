import sbt.Keys._

val akkaVersion = "2.3.5"
val scalamockVersion = "3.2.1"

val compileDependencies = Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2", // 3.x.x version acting weird
  "joda-time" % "joda-time" % "2.4",
  "org.yaml" % "snakeyaml" % "1.13",
  "com.jsuereth" %% "scala-arm" % "1.4",
  "org.scalanlp" %% "breeze" % "0.9" exclude("com.github.rwl", "jtransforms"),
  "com.typesafe.play" %% "play-json" % "2.3.3",
  "com.github.scopt" %% "scopt" % "3.2.0",
  "org.apache.commons" % "commons-lang3" % "3.3.2"
)

val runtimeDependencies = Seq(
  "ch.qos.logback" % "logback-classic" % "1.1.2" % "runtime"
)

val testDependencies = Seq(
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test, it",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test, it",
  "org.scalamock" %% "scalamock-core" % scalamockVersion % "test, it",
  "org.scalamock" %% "scalamock-scalatest-support" % scalamockVersion % "test, it",
//  "org.clapper" %% "grizzled-scala" % "1.2" % "test, it",
  "com.h2database" % "h2" % "1.4.181" % "test, it"
)

lazy val root = (project in file(".")).settings(
  organization := "eu.semberal",
  name := "dbstress",
  version := "1.0.0-beta3-SNAPSHOT",
  scalaVersion := "2.11.7",
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings", "-Xmax-classfile-name", "140")
).settings(resolvers ++= Seq(
  "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"
)).configs(IntegrationTest).settings(Defaults.itSettings: _*)
  .settings(packSettings: _*).settings(packMain := Map("dbstress" -> "eu.semberal.dbstress.Main"))
  .settings(libraryDependencies ++= (compileDependencies ++ runtimeDependencies ++ testDependencies): _*)
