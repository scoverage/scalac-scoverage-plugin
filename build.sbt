import sbtcrossproject.CrossProject
import sbtcrossproject.CrossType

lazy val munitVersion = "0.7.29"
lazy val scalametaVersion = "4.4.28"
lazy val defaultScala212 = "2.12.15"
lazy val defaultScala213 = "2.13.6"
lazy val defaultScala3 = "3.1.0"
lazy val bin212 =
  Seq(
    defaultScala212,
    "2.12.14",
    "2.12.13",
    "2.12.12",
    "2.12.11",
    "2.12.10",
    "2.12.9",
    "2.12.8"
  )
lazy val bin213 =
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
      ),
      Developer(
        "ckipp01",
        "Chris Kipp",
        "open-source@chris-kipp.io",
        url("https://www.chris-kipp.io/")
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
  }
)

lazy val root = Project("scalac-scoverage", file("."))
  .settings(
    name := "scalac-scoverage",
    publishArtifact := false,
    publishLocal := {}
  )
  .aggregate(plugin, runtime.jvm, runtime.js, reporter)

lazy val runtime = CrossProject(
  "runtime",
  file("runtime")
)(JVMPlatform, JSPlatform)
  .crossType(CrossType.Full)
  .withoutSuffixFor(JVMPlatform)
  .settings(
    name := "scalac-scoverage-runtime",
    crossScalaVersions := Seq(defaultScala212, defaultScala213),
    crossTarget := target.value / s"scala-${scalaVersion.value}",
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % munitVersion % Test
    ),
    sharedSettings
  )
  .jvmSettings(
    Test / fork := true
  )
  .jsSettings(
    scalaJSStage := FastOptStage
  )

lazy val `runtimeJVM` = runtime.jvm
lazy val `runtimeJS` = runtime.js

lazy val plugin =
  Project("plugin", file("plugin"))
    .dependsOn(runtimeJVM % Test)
    .settings(
      name := "scalac-scoverage-plugin",
      crossTarget := target.value / s"scala-${scalaVersion.value}",
      crossScalaVersions := bin212 ++ bin213,
      crossVersion := CrossVersion.full,
      libraryDependencies ++= Seq(
        "org.scala-lang.modules" %% "scala-xml" % "2.0.0",
        "org.scalameta" %% "munit" % munitVersion % Test,
        "org.scala-lang" % "scala-compiler" % scalaVersion.value % Provided
      ),
      sharedSettings
    )
    .settings(
      (Test / unmanagedSourceDirectories) += (Test / sourceDirectory).value / "scala-2.12+"
    )
    .dependsOn(reporter)

lazy val reporter =
  Project("reporter", file("reporter"))
    .settings(
      name := "scalac-scoverage-reporter",
      libraryDependencies ++= Seq(
        "org.scala-lang.modules" %% "scala-xml" % "2.0.0",
        "org.scalameta" %% "munit" % munitVersion % Test
      ),
      sharedSettings,
      crossScalaVersions := Seq(defaultScala212, defaultScala213, defaultScala3)
    )

addCommandAlias(
  "styleFix",
  "scalafixAll ; scalafmtAll ; scalafmtSbt"
)

addCommandAlias(
  "styleCheck",
  "scalafmtCheckAll ; scalafmtSbtCheck ; scalafix --check"
)
