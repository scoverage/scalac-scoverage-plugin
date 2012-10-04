organization := "reaktor"

name := "scct"

version := "0.2-SNAPSHOT"

scalaVersion := "2.9.2"

crossScalaVersions := Seq("2.9.2", "2.9.1-1", "2.9.1", "2.9.0-1", "2.9.0")

libraryDependencies <+= (scalaVersion) { v =>
  "org.scala-lang" % "scala-compiler" % v % "provided"
}

libraryDependencies ++= Seq(
  "it.unimi.dsi" % "fastutil" % "6.4.3" withSources,
  "junit" % "junit" % "4.7" % "test" withSources,
  "org.mockito" % "mockito-all" % "1.8.5" % "test" withSources,
  "org.specs2" % "specs2" % "1.11" % "test" cross CrossVersion.binaryMapped {
    case "2.9.0-1" => "2.9.1"
    case "2.9.0" => "2.9.1"
    case x => x
  }
)

publishTo := Some(Resolver.file("file",  new File("../gh-pages/maven-repo")))

resolvers += "scala-tools-releases" at "https://oss.sonatype.org/content/groups/scala-tools/"
