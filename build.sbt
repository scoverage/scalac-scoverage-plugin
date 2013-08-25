name := "scales"

organization := "com.sksamuel.scales"

version := "0.3.6-SNAPSHOT"

scalaVersion := "2.10.0"

crossScalaVersions := Seq("2.10.0")

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := {
  _ => false
}

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies += "org.scalatest" %% "scalatest" % "2.0.M5b" % "test"

libraryDependencies += "org.scala-lang" % "scala-compiler" % "2.10.2"

parallelExecution in Test := false

publishTo <<= version {
  (v: String) =>
    val nexus = "https://oss.sonatype.org/"
    if (v.trim.endsWith("SNAPSHOT"))
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

pomExtra := (
  <url>https://github.com/sksamuel/scales</url>
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:sksamuel/scales.git</url>
      <connection>scm:git@github.com:sksamuel/scales.git</connection>
    </scm>
    <developers>
      <developer>
        <id>sksamuel</id>
        <name>Stephen Samuel</name>
        <url>http://github.com/sksamuel</url>
      </developer>
    </developers>)