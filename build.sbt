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
  Seq(
    /* Akka */
    "com.typesafe.akka" % "akka-actor_2.11" % akkaVersion,
    "com.typesafe.akka" % "akka-slf4j_2.11" % akkaVersion,
    "com.typesafe.akka" % "akka-testkit_2.11" % akkaVersion % "test",
    /* Logging */
    "ch.qos.logback" % "logback-classic" % "1.1.2" % "runtime",
    "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2", // todo update to 3.x
    /* Java libraries */
    "joda-time" % "joda-time" % "2.3",
    "org.yaml" % "snakeyaml" % "1.13",
    /* Testing */
    "org.scalatest" %% "scalatest" % "2.2.0" % "test",
    /* Other */
    "com.jsuereth" % "scala-arm_2.11" % "1.4",
    "com.h2database" % "h2" % "1.4.178",
    "org.scalanlp" % "breeze_2.11" % "0.8.1",
    "com.typesafe.play" % "play-json_2.11" % "2.3.1",
    "com.github.scopt" % "scopt_2.11" % "3.2.0"
  )
}

// todo Uncomment once scalastyle for Scala 2.11 is released
//org.scalastyle.sbt.ScalastylePlugin.Settings
//
//lazy val testScalaStyle = taskKey[Unit]("testScalaStyle")
//
//testScalaStyle := {
//  org.scalastyle.sbt.PluginKeys.scalastyle.toTask("").value
//}
//
//(test in Test) <<= (test in Test) dependsOn testScalaStyle

XitrumPackage.copy("bin", "LICENSE.txt", "README.md")
