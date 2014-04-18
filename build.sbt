name := "scalac-scoverage-plugin"

organization := "org.scoverage"

version := "0.98.2"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")

libraryDependencies ++= Seq(
  "commons-io"          %     "commons-io"        % "2.4",
  "org.scala-lang"      %     "scala-compiler"    % "2.10.3"      % "provided",
  "org.scalatest"       %%    "scalatest"         % "2.1.0"       % "test",
  "org.mockito"         %     "mockito-all"       % "1.9.5"       % "test"
)

publishMavenStyle := true

publishArtifact in Test := false

parallelExecution in Test := false


pomIncludeRepository := {
  _ => false
}

javacOptions ++= Seq("-source", "1.6", "-target", "1.6")

scalaVersion := "2.10.3"

crossScalaVersions := Seq("2.10.3", "2.11.0-RC1")

libraryDependencies := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, scalaMajor)) if scalaMajor >= 11 =>
      libraryDependencies.value :+ "org.scala-lang.modules" %% "scala-xml" % "1.0.0"
    case _ =>
      libraryDependencies.value
  }
}

libraryDependencies := {
  val version = CrossVersion.binaryScalaVersion(scalaVersion.value)
  libraryDependencies.value :+ "org.scala-lang" % "scala-compiler" % version % "provided"
}

publishTo <<= version {
  (v: String) =>
    val nexus = "https://oss.sonatype.org/"
    if (v.trim.endsWith("SNAPSHOT"))
      Some(Resolver.file("file", new File(Path.userHome.absolutePath + "/.m2/repository")))
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

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
}