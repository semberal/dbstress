organization := "eu.semberal"

name := "dbstress"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.1"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings", "-Xmax-classfile-name", "140")

resolvers ++= Seq(
  "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= {
  val akkaVersion = "2.3.4"
  val scalamockVersion = "3.1.2"
  Seq(
    /* Akka */
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    /* Logging */
    "ch.qos.logback" % "logback-classic" % "1.1.2" % "runtime",
    "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
    /* Java libraries */
    "joda-time" % "joda-time" % "2.3",
    "org.yaml" % "snakeyaml" % "1.13",
    /* Testing */
    "org.scalatest" %% "scalatest" % "2.2.0" % "test",
    "org.scalamock" %% "scalamock-core" % scalamockVersion % "test",
    "org.scalamock" %% "scalamock-scalatest-support" % scalamockVersion % "test",
    "org.clapper" %% "grizzled-scala" % "1.2" % "test",
    "com.h2database" % "h2" % "1.4.178" % "test",
    /* Other */
    "com.jsuereth" %% "scala-arm" % "1.4",
    "org.scalanlp" %% "breeze" % "0.8.1",
    "com.typesafe.play" %% "play-json" % "2.3.1",
    "com.github.scopt" %% "scopt" % "3.2.0"
  )
}

XitrumPackage.copy("bin", "LICENSE.txt", "README.md")
