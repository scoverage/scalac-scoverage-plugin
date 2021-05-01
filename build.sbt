import sbtcrossproject.CrossProject
import sbtcrossproject.CrossType

def localSnapshotVersion = "1.4.5-SNAPSHOT"
def isCI = System.getenv("CI") != null

val ScalatestVersion = "3.1.1"

val bin212 = Seq("2.12.13", "2.12.12", "2.12.11", "2.12.10", "2.12.9", "2.12.8")
val bin213 = Seq("2.13.5", "2.13.4", "2.13.3", "2.13.2", "2.13.1", "2.13.0")

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
    version ~= { dynVer =>
      if (isCI) dynVer
      else localSnapshotVersion // only for local publishing
    },
    scalaVersion := bin213.head,
    crossScalaVersions := bin212 ++ bin213,
    versionScheme := Some("early-semver"),
    Test / fork := false,
    Test / publishArtifact := false,
    Test / parallelExecution := false,
    scalacOptions := Seq(
      "-unchecked",
      "-deprecation",
      "-feature",
      "-encoding",
      "utf8"
    ),
    Global / concurrentRestrictions += Tags.limit(Tags.Test, 1),
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value % Compile
    )
  )
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
    crossVersion := CrossVersion.full,
    libraryDependencies += "org.scalatest" %%% "scalatest" % ScalatestVersion % Test
  )
  .jvmSettings(
    Test / fork := true
  )
  .jsSettings(
    crossVersion := CrossVersion
      .fullWith("sjs" + scalaJSVersion.take(1) + "_", ""),
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
        "org.scala-lang.modules" %% "scala-xml" % "1.2.0",
        "org.scalatest" %% "scalatest" % ScalatestVersion % Test
      )
    )
    .settings(
      (Test / unmanagedSourceDirectories) += (Test / sourceDirectory).value / "scala-2.12+"
    )
