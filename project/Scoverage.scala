import sbt.Keys._
import sbt._
import com.typesafe.sbt.pgp.PgpKeys
import org.scalajs.sbtplugin.cross.CrossProject
import org.scalajs.sbtplugin.cross.CrossType
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

object Scoverage extends Build {

  val Org = "org.scoverage"
  val MockitoVersion = "1.10.19"
  val ScalatestVersion = "3.0.0"

  lazy val LocalTest = config("local") extend Test

  val appSettings = Seq(
    organization := Org,
    scalaVersion := "2.11.8",
    crossScalaVersions := Seq("2.10.6", scalaVersion.value, "2.12.0-RC1"),
    fork in Test := false,
    publishMavenStyle := true,
    publishArtifact in Test := false,
    parallelExecution in Test := false,
    sbtrelease.ReleasePlugin.autoImport.releasePublishArtifactsAction := PgpKeys.publishSigned.value,
    sbtrelease.ReleasePlugin.autoImport.releaseCrossBuild := true,
    scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8"),
    resolvers := ("releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2") +: resolvers.value,
    concurrentRestrictions in Global += Tags.limit(Tags.Test, 1),
    publishTo <<= version {
      (v: String) =>
        val nexus = "https://oss.sonatype.org/"
        if (v.trim.endsWith("SNAPSHOT"))
          Some(Resolver.sonatypeRepo("snapshots"))
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
  )

  lazy val root = Project("scalac-scoverage", file("."))
    .settings(name := "scalac-scoverage")
    .settings(appSettings: _*)
    .settings(publishArtifact := false)
    .aggregate(plugin, runtime.jvm, runtime.js)

  lazy val runtime = CrossProject("scalac-scoverage-runtime", file("scalac-scoverage-runtime"), CrossType.Full)
    .settings(name := "scalac-scoverage-runtime")
    .settings(appSettings: _*)
    .jvmSettings(
      libraryDependencies ++= Seq(
      "org.mockito" % "mockito-all" % MockitoVersion % Test,
      "org.scalatest" %% "scalatest" % ScalatestVersion % Test
      )
    )
    .jsSettings(
      libraryDependencies += "org.scalatest" %%% "scalatest" % ScalatestVersion,
      scalaJSStage := FastOptStage
    )

  lazy val `scalac-scoverage-runtimeJVM` = runtime.jvm
  lazy val `scalac-scoverage-runtimeJS` = runtime.js

  lazy val plugin = Project("scalac-scoverage-plugin", file("scalac-scoverage-plugin"))
    .dependsOn(`scalac-scoverage-runtimeJVM` % Test)
    .settings(name := "scalac-scoverage-plugin")
    .settings(appSettings: _*)
    .settings(crossVersionSharedSources)
    .settings(libraryDependencies ++= Seq(
      "org.mockito"                %  "mockito-all"    % MockitoVersion     % Test,
      "org.scalatest"              %% "scalatest"      % ScalatestVersion   % Test,
      "org.scala-lang"             %  "scala-reflect"  % scalaVersion.value % "provided",
      "org.scala-lang"             %  "scala-compiler" % scalaVersion.value % "provided",
      "org.joda"                   %  "joda-convert"   % "1.8.1"            % Test,
      "joda-time"                  %  "joda-time"      % "2.9.4"            % Test
    )).settings(libraryDependencies ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, scalaMajor)) if scalaMajor > 10 =>
        Seq("org.scala-lang.modules" %% "scala-xml" % "1.0.5")
      case _ =>
        Seq()
    }
  })

  lazy val crossVersionSharedSources: Seq[Setting[_]] =
  Seq(Compile, Test).map { sc =>
    (unmanagedSourceDirectories in sc) ++= {
      (unmanagedSourceDirectories in sc ).value.map { dir: File =>
        CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((2, y)) if y == 10 => new File(dir.getPath + "_2.10")
          case Some((2, y)) if y >= 11 => new File(dir.getPath + "_2.11+")
        }
      }
    }
  }

}
