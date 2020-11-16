import sbt._
import sbt.Keys._
import sbtrelease.ReleasePlugin.autoImport._
import sbtcrossproject.CrossProject
import sbtcrossproject.CrossType

val Org = "org.scoverage"
val ScalatestVersion = "3.1.1"

val appSettings = Seq(
    organization := Org,
    scalaVersion := "2.12.10",
    crossScalaVersions := Seq("2.12.10", "2.13.3"),
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
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value % Compile
    ),
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
    .settings(
      libraryDependencies += "org.scalatest" %%% "scalatest" % ScalatestVersion % Test
    )
    .jvmSettings(
      fork in Test := true
    )
    .jsSettings(
      scalaJSStage := FastOptStage
    )

lazy val `scalac-scoverage-runtimeJVM` = runtime.jvm
lazy val `scalac-scoverage-runtimeJS` = runtime.js

lazy val plugin = Project("scalac-scoverage-plugin", file("scalac-scoverage-plugin"))
    .dependsOn(`scalac-scoverage-runtimeJVM` % Test)
    .settings(name := "scalac-scoverage-plugin")
    .settings(appSettings: _*)
    .settings(
      libraryDependencies ++= Seq(
        "org.scala-lang.modules" %% "scala-xml" % "1.2.0",
        "org.scalatest" %% "scalatest" % ScalatestVersion % Test
      )
    )
  .settings(
    unmanagedSourceDirectories in Test += (sourceDirectory in Test).value / "scala-2.12+"
  )

