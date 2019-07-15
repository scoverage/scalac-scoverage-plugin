import sbt._
import sbt.Keys._
import sbtrelease.ReleasePlugin.autoImport._
import com.typesafe.sbt.pgp.PgpKeys
import sbtcrossproject.CrossProject
import sbtcrossproject.CrossType

val Org = "org.scoverage"
val ScalatestVersion = "3.0.8"

val appSettings = Seq(
    organization := Org,
    scalaVersion := "2.12.8",
    crossScalaVersions := Seq("2.10.7", "2.11.12", "2.12.8", "2.13.0"),
    fork in Test := false,
    publishMavenStyle := true,
    publishArtifact in Test := false,
    parallelExecution in Test := false,
    scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8"),
    concurrentRestrictions in Global += Tags.limit(Tags.Test, 1),
    publishTo := {
      if (isSnapshot.value)
        Some("snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")
      else
        Some("releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2")
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
          <developer>
            <id>gslowikowski</id>
            <name>Grzegorz Slowikowski</name>
            <url>http://github.com/gslowikowski</url>
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
    .settings(publishArtifact := false)
    .settings(publishLocal := {})
    .aggregate(plugin, runtime.jvm, runtime.js)

lazy val runtime = CrossProject("scalac-scoverage-runtime", file("scalac-scoverage-runtime"))(JVMPlatform, JSPlatform)
    .crossType(CrossType.Full)
    .settings(name := "scalac-scoverage-runtime")
    .settings(appSettings: _*)
    .jvmSettings(
      fork in Test := true,
      libraryDependencies ++= Seq(
        "org.scalatest" %% "scalatest" % ScalatestVersion % "test"
      )
    )
    .jsSettings(
      libraryDependencies += "org.scalatest" %%% "scalatest" % ScalatestVersion % "test",
      scalaJSStage := FastOptStage
    )

lazy val `scalac-scoverage-runtimeJVM` = runtime.jvm
lazy val `scalac-scoverage-runtimeJS` = runtime.js

lazy val plugin = Project("scalac-scoverage-plugin", file("scalac-scoverage-plugin"))
    .dependsOn(`scalac-scoverage-runtimeJVM` % "test")
    .settings(name := "scalac-scoverage-plugin")
    .settings(appSettings: _*)
    .settings(
      libraryDependencies ++= Seq(
        "org.scalatest" %% "scalatest" % ScalatestVersion % "test",
        "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided"
      )
    )
    .settings(
      unmanagedSourceDirectories in Test ++= {
        CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((2, scalaMajor)) if scalaMajor > 10 =>
            Seq((sourceDirectory in Test).value / "scala-2.11+")
          case _ =>
            Seq()
        }
      },
      libraryDependencies ++= {
        CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((2, scalaMajor)) if scalaMajor > 10 =>
            Seq("org.scala-lang.modules" %% "scala-xml" % "1.2.0")
          case _ =>
            Seq()
        }
      }
    )
