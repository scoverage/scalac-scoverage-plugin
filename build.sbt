import sbtcrossproject.CrossProject
import sbtcrossproject.CrossType

val scalatestVersion = "3.2.9"
val scalametaVersion = "4.4.20"
val defaultScala213 = "2.13.6"
val bin211 = Seq("2.11.12")
val bin212 =
  Seq("2.12.14", "2.12.13", "2.12.12", "2.12.11", "2.12.10", "2.12.9", "2.12.8")
val bin213 =
  Seq(
    defaultScala213,
    "2.13.5",
    "2.13.4",
    "2.13.3",
    "2.13.2",
    "2.13.1",
    "2.13.0"
  )

inThisBuild(
  List(
    organization := "org.scoverage",
    homepage := Some(url("http://scoverage.org/")),
    developers := List(
      Developer(
        "sksamuel",
        "Stephen Samuel",
        "sam@sksamuel.com",
        url("https://github.com/sksamuel")
      ),
      Developer(
        "gslowikowski",
        "Grzegorz Slowikowski",
        "gslowikowski@gmail.com",
        url("https://github.com/gslowikowski")
      )
    ),
    licenses := Seq(
      "Apache-2.0" -> url("http://www.apache.org/license/LICENSE-2.0")
    ),
    scalaVersion := defaultScala213,
    versionScheme := Some("early-semver"),
    Test / fork := false,
    Test / publishArtifact := false,
    Test / parallelExecution := false,
    Global / concurrentRestrictions += Tags.limit(Tags.Test, 1),
    scalacOptions := Seq(
      "-unchecked",
      "-deprecation",
      "-feature",
      "-encoding",
      "utf8"
    ),
    scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.5.0",
    semanticdbEnabled := true,
    semanticdbVersion := scalametaVersion,
    scalafixScalaBinaryVersion := scalaBinaryVersion.value
  )
)

lazy val sharedSettings = List(
  scalacOptions := {
    if (scalaVersion.value == defaultScala213) {
      scalacOptions.value :+ "-Wunused:imports"
    } else {
      scalacOptions.value
    }
  },
  crossScalaVersions := bin211 ++ bin212 ++ bin213
)

lazy val root = Project("scalac-scoverage", file("."))
  .settings(
    name := "scalac-scoverage",
    publishArtifact := false,
    publishLocal := {}
  )
  .aggregate(plugin, runtime.jvm, runtime.js)

lazy val runtime = CrossProject(
  "scalac-scoverage-runtime",
  file("scalac-scoverage-runtime")
)(JVMPlatform, JSPlatform)
  .crossType(CrossType.Full)
  .withoutSuffixFor(JVMPlatform)
  .settings(
    name := "scalac-scoverage-runtime",
    crossTarget := target.value / s"scala-${scalaVersion.value}",
    libraryDependencies ++= Seq(
      "org.scalatest" %%% "scalatest" % scalatestVersion % Test
    ),
    sharedSettings
  )
  .jvmSettings(
    Test / fork := true
  )
  .jsSettings(
    scalaJSStage := FastOptStage
  )

lazy val `scalac-scoverage-runtimeJVM` = runtime.jvm
lazy val `scalac-scoverage-runtimeJS` = runtime.js

lazy val plugin =
  Project("scalac-scoverage-plugin", file("scalac-scoverage-plugin"))
    .dependsOn(`scalac-scoverage-runtimeJVM` % Test)
    .settings(
      name := "scalac-scoverage-plugin",
      crossTarget := target.value / s"scala-${scalaVersion.value}",
      crossVersion := CrossVersion.full,
      libraryDependencies ++= Seq(
        "org.scala-lang.modules" %% "scala-xml" % "2.0.0",
        "org.scalatest" %% "scalatest" % scalatestVersion % Test,
        "org.scala-lang" % "scala-compiler" % scalaVersion.value % Provided
      ),
      sharedSettings
    )
    .settings(
      (Test / unmanagedSourceDirectories) += (Test / sourceDirectory).value / "scala-2.12+"
    )

addCommandAlias(
  "styleFix",
  "scalafixAll ; scalafmtAll ; scalafmtSbt"
)

addCommandAlias(
  "styleCheck",
  "scalafmtCheckAll ; scalafmtSbtCheck ; scalafix --check"
)
