name := """WDJ"""

version := "1.0"

scalaVersion := "2.11.7"

crossScalaVersions := Seq("2.10.6", "2.11.6", "2.11.7")

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8", // yes, this is 2 args
  "-feature",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint",
  "-Yno-adapted-args",
  // "-Ywarn-dead-code", // N.B. doesn't work well with the ??? hole
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Xfuture")

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % "7.2.0",
  "org.scalaz" %% "scalaz-scalacheck-binding" % "7.2.0" % "test",
  "org.scalacheck" %% "scalacheck" % "1.12.5"
)

initialCommands in console := """
  import com.localytics.WDJ
  import com.localytics.syntax.wdj._
  import scalaz.std.anyVal._
  import scalaz.std.string._
  import scalaz.std.map._
  import scalaz.syntax.either._
  import scalaz.syntax.writer._
"""
tutSettings
