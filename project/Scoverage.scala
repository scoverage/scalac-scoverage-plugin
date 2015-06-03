import sbt.Keys._
import sbt._
import sbtrelease.ReleasePlugin
import sbtrelease.ReleasePlugin.ReleaseKeys
import com.typesafe.sbt.pgp.PgpKeys
import org.scalajs.sbtplugin.cross.CrossProject
import org.scalajs.sbtplugin.cross.CrossType
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

object Scoverage extends Build {

  val Org = "org.scoverage"
  val Scala = "2.11.7"
  val MockitoVersion = "1.9.5"
  val ScalatestVersion = "3.0.0-M15"

  lazy val LocalTest = config("local") extend Test

  val appSettings = Seq(
    organization := Org,
    scalaVersion := Scala,
    crossScalaVersions := Seq("2.10.6", "2.11.7"),
    fork in Test := false,
    publishMavenStyle := true,
    publishArtifact in Test := false,
    parallelExecution in Test := false,
    scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8"),
    resolvers := ("releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2") +: resolvers.value,
    concurrentRestrictions in Global += Tags.limit(Tags.Test, 1),
    javacOptions := Seq("-source", "1.6", "-target", "1.6"),
    javaOptions += "-XX:MaxMetaspaceSize=2048m",
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
  ) ++ ReleasePlugin.releaseSettings ++ Seq(
    ReleaseKeys.crossBuild := true,
    ReleaseKeys.publishArtifactsAction := PgpKeys.publishSigned.value
  )

  lazy val root = Project("scalac-scoverage", file("."))
    .settings(name := "scalac-scoverage")
    .settings(appSettings: _*)
    .settings(publishArtifact := false)
    .aggregate(plugin, runtime.jvm, runtime.js)

  lazy val runtime = CrossProject("scalac-scoverage-runtime", file("scalac-scoverage-runtime"), CrossType.Full)
    .settings(name := "scalac-scoverage-runtime")
    .settings(appSettings: _*)
    .jvmSettings(libraryDependencies ++= Seq(
      "org.mockito" % "mockito-all" % MockitoVersion % "test",
      "org.scalatest" %% "scalatest" % ScalatestVersion % "test"
    ))
    .jsSettings(
      libraryDependencies += "org.scalatest" %%% "scalatest" % ScalatestVersion,
      scalaJSStage := FastOptStage
    )

  lazy val `scalac-scoverage-runtimeJVM` = runtime.jvm
  lazy val `scalac-scoverage-runtimeJS` = runtime.js

  lazy val plugin = Project("scalac-scoverage-plugin", file("scalac-scoverage-plugin"))
    .dependsOn(`scalac-scoverage-runtimeJVM` % "test")
    .settings(name := "scalac-scoverage-plugin")
    .settings(appSettings: _*)
    .settings(libraryDependencies ++= Seq(
    "org.mockito" % "mockito-all" % MockitoVersion % "test",
    "org.scalatest" %% "scalatest" % ScalatestVersion % "test",
    "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided",
    "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided",
    "org.joda" % "joda-convert" % "1.6" % "test",
    "joda-time" % "joda-time" % "2.3" % "test",
    "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2" % "test"
  )).settings(libraryDependencies ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, scalaMajor)) if scalaMajor == 11 =>
        EnvSupport.setEnv("CrossBuildScalaVersion", "2.11.7")
        Seq("org.scala-lang.modules" %% "scala-xml" % "1.0.4")
      case _ =>
        EnvSupport.setEnv("CrossBuildScalaVersion", "2.10.6")
        Nil
    }
  })
}
