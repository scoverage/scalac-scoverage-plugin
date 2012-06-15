organization := "reaktor"

name := "scct"

version := "0.2-SNAPSHOT"

scalaVersion := "2.9.2"

crossScalaVersions := Seq("2.9.2", "2.9.1-1", "2.9.1", "2.9.0-1", "2.9.0")

libraryDependencies <+= (scalaVersion) { v =>
  "org.scala-lang" % "scala-compiler" % v % "provided"
}

libraryDependencies ++= Seq(
  "junit" % "junit" % "4.7" % "test" withSources,
  "org.mockito" % "mockito-all" % "1.8.5" % "test" withSources,
  "org.scala-tools.testing" % "specs_2.9.1" % "1.6.9" % "test" withSources
)

publishTo := Some(Resolver.file("file",  new File("../gh-pages/maven-repo")))

