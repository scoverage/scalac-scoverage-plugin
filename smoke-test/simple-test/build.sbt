organization := "reaktor.scct"

name := "simple-test"

version := "1.0"

scalaVersion := "2.9.2"

libraryDependencies ++= Seq(
  "junit" % "junit" % "4.10" % "test",
  "org.specs2" %% "specs2" % "1.9" % "test"
)

// logLevel := Level.Debug
