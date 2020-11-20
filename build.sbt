import sbt.Keys._

val dependencies = Seq(
  "com.typesafe.akka" %% "akka-actor" % Versions.akka,
  "com.typesafe.akka" %% "akka-slf4j" % Versions.akka,
  "com.typesafe.scala-logging" %% "scala-logging" % Versions.scalaLogging,
  "org.yaml" % "snakeyaml" % Versions.snakeYaml,
  "com.github.pathikrit" %% "better-files" % Versions.betterFiles,
  "com.github.tototoshi" %% "scala-csv" % Versions.scalaCsv,
  "org.apache.commons" % "commons-math3" % Versions.commonsMath,
  "com.github.scopt" %% "scopt" % Versions.scopt,
  "org.apache.commons" % "commons-lang3" % Versions.commonsLang3,
  "ch.qos.logback" % "logback-classic" % Versions.logbackClassic,
  "com.typesafe.akka" %% "akka-testkit" % Versions.akka % "test, it",
  "org.scalatest" %% "scalatest" % Versions.scalatest % "test, it",
  "org.scalatest" %% "scalatest-flatspec" % Versions.scalatest % "test, it",
  "org.postgresql" % "postgresql" % Versions.postgres % "test, it"
)

lazy val dbstresss = (project in file("."))
  .enablePlugins(PackPlugin)
  .settings(
    organization := "eu.semberal",
    name := "dbstress",
    version := "1.1.0-SNAPSHOT",
    scalaVersion := Versions.scala,
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-feature",
      "-Xfatal-warnings",
      "-Ywarn-unused:imports"
    )
  )
  .configs(IntegrationTest)
  .settings(Defaults.itSettings: _*)
  .settings(libraryDependencies ++= dependencies: _*)
  .settings(packMain := Map("dbstress" -> "eu.semberal.dbstress.Main"))
  .settings(packArchiveExcludes := List("VERSION", "Makefile"))
  .settings(
    inConfig(IntegrationTest)(
      org.scalafmt.sbt.ScalafmtPlugin.scalafmtConfigSettings
    )
  )
