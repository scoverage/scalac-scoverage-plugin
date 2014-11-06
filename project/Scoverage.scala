import sbt.Keys._
import sbt._

object Scoverage extends Build {

  val Org = "org.scoverage"
  val Version = "1.0.0.SNAPSHOT"
  val Scala = "2.11.4"
  val Slf4jVersion = "1.7.7"
  val ScrimageVersion = "1.4.2"
  val ScalatestVersion = "2.2.2"

  lazy val LocalTest = config("local") extend Test

  val appSettings = Seq(
    name := "scalac-scoverage",
    version := Version,
    organization := Org,
    scalaVersion := Scala,
    crossScalaVersions := Seq("2.10.4", "2.11.4"),
    fork in Test := false,
    publishMavenStyle := true,
    publishArtifact in Test := false,
    parallelExecution in Test := false,
    scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8"),
    resolvers := ("releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2") +: resolvers.value,
    concurrentRestrictions in Global += Tags.limit(Tags.Test, 1),
    scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8"),
    javacOptions := Seq("-source", "1.6", "-target", "1.6"),
    libraryDependencies ++= Seq(
      "org.slf4j" % "slf4j-api" % Slf4jVersion,
      "commons-io" % "commons-io" % "2.4",
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "org.joda" % "joda-convert" % "1.6" % "test",
      "joda-time" % "joda-time" % "2.3" % "test",
      "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2" % "test",
      "org.mockito" % "mockito-all" % "1.9.5" % "test",
      "org.scalatest" %% "scalatest" % ScalatestVersion % "test"
    ),
    libraryDependencies := {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, scalaMajor)) if scalaMajor == 11 =>
          EnvSupport.setEnv("CrossBuildScalaVersion", "2.11.4")
          libraryDependencies.value :+ "org.scala-lang.modules" %% "scala-xml" % "1.0.1"
        case _ =>
          EnvSupport.setEnv("CrossBuildScalaVersion", "2.10.4")
          libraryDependencies.value
      }
    },
    publishTo <<= version {
      (v: String) =>
        val nexus = "https://oss.sonatype.org/"
        if (v.trim.endsWith("SNAPSHOT"))
          Some(Resolver.file("file", new File(Path.userHome.absolutePath + "/.m2/repository")))
        else
          Some("releases" at nexus + "service/local/staging/deploy/maven2")
    }
  )

  lazy val root = Project("scalac-scoverage", file("."))
    .settings(appSettings: _*)
    .aggregate(plugin, runtime)

  lazy val plugin = Project("scalac-scoverage-plugin", file("scalac-scoverage-plugin"))
    .settings(appSettings: _*)

  lazy val runtime = Project("scalac-scoverage-runtime", file("scalac-scoverage-runtime"))
    .settings(appSettings: _*)
}