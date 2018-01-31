name := "smobs"
organization := "com.github.ashashev"
version := "0.0.2"
scalaVersion := "2.12.4"

scalacOptions += "-unchecked"
scalacOptions += "-deprecation"
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.4" % "test",
  "com.github.scopt" %% "scopt" % "3.7.0",
  "org.scalaj" %% "scalaj-http" % "2.3.0",
  "org.json4s" %% "json4s-native" % "3.5.0"
)
