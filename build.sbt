organization := "reaktor"

name := "scct"

version := "0.2-SNAPSHOT"

scalaVersion := "2.10.0-RC3"

crossScalaVersions := Seq("2.10.0-RC3", "2.9.2", "2.9.1-1", "2.9.1", "2.9.0-1", "2.9.0")

libraryDependencies <+= (scalaVersion) { v =>
  "org.scala-lang" % "scala-compiler" % v % "provided"
}

libraryDependencies ++= Seq(
  "junit" % "junit" % "4.10" % "test",
  "org.mockito" % "mockito-all" % "1.9.5-rc1" % "test" withSources,
  "org.specs2" % "specs2" % "1.12.3" % "test" cross CrossVersion.binaryMapped {
    case "2.9.0-1" => "2.9.1"
    case "2.9.0" => "2.9.1"
    case x => x
  }
)

publishTo := Some(Resolver.file("file",  new File("../gh-pages/maven-repo")))

resolvers += "scala-tools-releases" at "https://oss.sonatype.org/content/groups/scala-tools/"

testOptions in Test <+= (scalaVersion in Test) map { (scalaVer) => Tests.Setup { () => System.setProperty("scct-test-scala-version", scalaVer) } }