import sbtcrossproject.CrossProject
import sbtcrossproject.CrossType

lazy val munitVersion = "0.7.29"
lazy val scalametaVersion = "4.5.9"
lazy val defaultScala212 = "2.12.16"
lazy val defaultScala213 = "2.13.8"
lazy val defaultScala3 = "3.1.0"
lazy val bin212 =
  Seq(
    defaultScala212,
    "2.12.15",
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
    "2.13.7",
    "2.13.6",
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
    scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0",
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
  libraryDependencies += "org.scalameta" %%% "munit" % munitVersion % Test
)

lazy val root = Project("scalac-scoverage", file("."))
  .settings(
    name := "scalac-scoverage",
    publishArtifact := false,
    publishLocal := {}
  )
  .aggregate(
    plugin,
    runtime.jvm,
    runtime.js,
    runtime.native,
    runtimeJSDOMTest,
    reporter,
    domain,
    serializer,
    buildInfo
  )

lazy val runtime = CrossProject(
  "runtime",
  file("runtime")
)(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .withoutSuffixFor(JVMPlatform)
  .settings(
    name := "scalac-scoverage-runtime",
    crossScalaVersions := Seq(defaultScala212, defaultScala213),
    crossTarget := target.value / s"scala-${scalaVersion.value}",
    sharedSettings
  )
  .jvmSettings(
    Test / fork := true
  )

lazy val `runtimeJVM` = runtime.jvm
lazy val `runtimeJS` = runtime.js

lazy val runtimeJSDOMTest =
  project
    .enablePlugins(ScalaJSPlugin)
    .dependsOn(runtimeJS % "test->test")
    .settings(
      publishArtifact := false,
      publishLocal := {},
      jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv(),
      sharedSettings
    )

lazy val plugin =
  project
    // we need both runtimes compiled prior to running tests
    .dependsOn(runtimeJVM % Test, runtimeJS % Test)
    .settings(
      name := "scalac-scoverage-plugin",
      crossTarget := target.value / s"scala-${scalaVersion.value}",
      crossScalaVersions := bin212 ++ bin213,
      crossVersion := CrossVersion.full,
      libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value % Provided,
      sharedSettings
    )
    .settings(
      Test / unmanagedSourceDirectories += (Test / sourceDirectory).value / "scala-2.12+"
    )
    .dependsOn(domain, reporter % "test->compile", serializer, buildInfo % Test)

lazy val reporter =
  project
    .settings(
      name := "scalac-scoverage-reporter",
      libraryDependencies += CrossVersion
        .partialVersion(
          scalaVersion.value
        )
        .map {
          // Lock this for 2.12 to align with the compiler
          // https://github.com/scala/scala/pull/9743
          case ((2, 12)) => "org.scala-lang.modules" %% "scala-xml" % "1.3.0"
          case _         => "org.scala-lang.modules" %% "scala-xml" % "2.1.0"
        },
      sharedSettings,
      crossScalaVersions := Seq(defaultScala212, defaultScala213, defaultScala3)
    )
    .dependsOn(domain, serializer)

lazy val buildInfo =
  project
    .settings(
      crossScalaVersions := bin212 ++ bin213,
      buildInfoKeys += BuildInfoKey("scalaJSVersion", scalaJSVersion),
      publishArtifact := false,
      publishLocal := {}
    )
    .enablePlugins(BuildInfoPlugin)

lazy val domain =
  project
    .settings(
      name := "scalac-scoverage-domain",
      sharedSettings,
      crossScalaVersions := Seq(defaultScala212, defaultScala213, defaultScala3)
    )

lazy val serializer =
  project
    .settings(
      name := "scalac-scoverage-serializer",
      sharedSettings,
      crossScalaVersions := Seq(defaultScala212, defaultScala213, defaultScala3)
    )
    .dependsOn(domain)

addCommandAlias(
  "styleFix",
  "scalafixAll ; scalafmtAll ; scalafmtSbt"
)

addCommandAlias(
  "styleCheck",
  "scalafmtCheckAll ; scalafmtSbtCheck ; scalafix --check"
)
