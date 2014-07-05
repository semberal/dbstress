organization := "eu.semberal"

name := "dbstress"

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.4"

resolvers ++= Seq(
  "duh.org sonatype oss repo" at "https://oss.sonatype.org/content/repositories/orgduh-1000/",
  "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"
)

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings")

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.3",
  "org.yaml" % "snakeyaml" % "1.13",
  "org.scalatest" %% "scalatest" % "2.2.0" % "test",
  "org.duh" %% "scala-resource-simple" % "0.3",
  "com.h2database" % "h2" % "1.4.178",
  "ch.qos.logback" % "logback-classic" % "1.1.2" % "runtime",
  "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
  "org.slf4j" % "slf4j-api" % "1.7.7",
  "org.scalanlp" %% "breeze" % "0.8.1",
  "com.typesafe.play" %% "play-json" % "2.3.1",
  "joda-time" % "joda-time" % "2.3"
)

org.scalastyle.sbt.ScalastylePlugin.Settings

lazy val testScalaStyle = taskKey[Unit]("testScalaStyle")

testScalaStyle := {
  org.scalastyle.sbt.PluginKeys.scalastyle.toTask("").value
}

(test in Test) <<= (test in Test) dependsOn testScalaStyle