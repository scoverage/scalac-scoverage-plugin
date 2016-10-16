import sbt._
import sbt.Keys._
import sbtrelease.ReleasePlugin.autoImport._
import com.typesafe.sbt.pgp.PgpKeys


val Org = "org.scoverage"
val ScalatestVersion = "3.0.0"

val appSettings = Seq(
  organization := Org,
  scalaVersion := "2.11.8",
  crossScalaVersions := Seq("2.10.6", "2.11.8", "2.12.0-RC1"),
  fork in Test := false,
  publishMavenStyle := true,
  publishArtifact in Test := false,
  parallelExecution in Test := false,
  scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8"),
  concurrentRestrictions in Global += Tags.limit(Tags.Test, 1),
  publishTo := {
    val nexus = "https://oss.sonatype.org/"

    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  pomExtra := {
    <url>https://github.com/scoverage/scalac-scoverage-plugin</url>
      <licenses>
        <license>
          <name>Apache 2</name>
          <url>http://www.apache.org/licenses/LICENSE-2.0</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:scoverage/scalac-scoverage-plugin.git</url>
        <connection>scm:git@github.com:scoverage/scalac-scoverage-plugin.git</connection>
      </scm>
      <developers>
        <developer>
          <id>sksamuel</id>
          <name>Stephen Samuel</name>
          <url>http://github.com/sksamuel</url>
        </developer>
      </developers>
  },
  pomIncludeRepository := {
    _ => false
  }
) ++ Seq(
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value
)

lazy val root = Project("scalac-scoverage", file("."))
  .settings(name := "scalac-scoverage")
  .settings(appSettings: _*)
  .settings(noPublishSettings)
  .aggregate(reporting)

lazy val noPublishSettings = Seq(
  publishArtifact := false,
  // The above is enough for Maven repos but it doesn't prevent publishing of ivy.xml files
  publish := {},
  publishLocal := {}
)

lazy val reporting = Project("scalac-scoverage-reporting", file("scalac-scoverage-reporting"))
  .settings(name := "scalac-scoverage-reporting")
  .settings(appSettings: _*)
  .settings(libraryDependencies ++= Seq(
    Org %% "scalac-scoverage-plugin" % "2.0.0-M0" cross CrossVersion.full,
    "org.scalatest" %% "scalatest" % ScalatestVersion % "test",
    "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided"
  ))
