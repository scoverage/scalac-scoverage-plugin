organization := "reaktor.scct"

name := "simple-test"

version := "1.0"

scalaVersion := "2.9.1"

resolvers += "scct-local-repo" at "file://"+baseDirectory+"/../../gh-pages/maven-repo"

libraryDependencies ++= Seq(
  "junit" % "junit" % "4.10" % "test",
  "org.specs2" %% "specs2" % "1.9" % "test"
)

seq(ScctPlugin.scctSettings: _*)
