name := "scalac-scoverage-plugin"

organization := "com.sksamuel.scoverage"

version := "0.95.8"

scalaVersion := "2.10.3"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")

crossScalaVersions := Seq("2.9.2", "2.10.0")

libraryDependencies ++= Seq(
  "commons-io"          % "commons-io"        % "2.4",
  "org.scala-lang"      % "scala-compiler"    % "2.10.3"  % "provided",
  "org.scalatest"       %% "scalatest"        % "2.0"     % "test",
  "org.mockito"         % "mockito-all"       % "1.9.5"   % "test",
  "org.specs2"          %% "specs2"           % "2.3.7"   % "test"
)

publishMavenStyle := true

publishArtifact in Test := false

parallelExecution in Test := false

pomIncludeRepository := {
  _ => false
}

javacOptions ++= Seq("-source", "1.6", "-target", "1.6")

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