organization := "eu..semberal"

name := "dbstress"

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.4"

resolvers += "duh.org sonatype oss repo" at "https://oss.sonatype.org/content/repositories/orgduh-1000/"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.3"

libraryDependencies += "org.yaml" % "snakeyaml" % "1.13"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.0" % "test"

libraryDependencies += "org.duh" %% "scala-resource-simple" % "0.3"

libraryDependencies += "com.h2database" % "h2" % "1.4.178"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.2" % "runtime"

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2"

libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.7"

libraryDependencies += "org.scalanlp" %% "breeze" % "0.8.1"
