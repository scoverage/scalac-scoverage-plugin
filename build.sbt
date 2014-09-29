import sbt.Keys._

name := "scalac-scoverage-plugin"

organization := "org.scoverage"

version := "0.99.9"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")

resolvers := ("releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2") +: resolvers.value

javacOptions ++= Seq("-source", "1.6", "-target", "1.6")

scalaVersion := "2.11.1"

crossScalaVersions := Seq("2.10.4", "2.11.1")

libraryDependencies ++= Seq(
  "org.scala-lang"                 %     "scala-compiler"        % scalaVersion.value    % "provided",
  "commons-io"                     %     "commons-io"            % "2.4",
  "org.scalatest"                  %%    "scalatest"             % "2.2.1"       % "test",
  "com.typesafe.scala-logging"     %%    "scala-logging-slf4j"   % "2.1.2"       % "test",
  "org.mockito"                    %     "mockito-all"           % "1.9.5"       % "test",
  "joda-time"                      %     "joda-time"             % "2.3"         % "test",
  "org.joda"                       %     "joda-convert"          % "1.3.1"       % "test",
  "org.slf4j"                      %     "slf4j-api"             % "1.7.7"       % "test"
)

libraryDependencies := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, scalaMajor)) if scalaMajor == 11 =>
      EnvSupport.setEnv("CrossBuildScalaVersion", "2.11.2")
      libraryDependencies.value :+ "org.scala-lang.modules" %% "scala-xml" % "1.0.1"
    case _ =>
      EnvSupport.setEnv("CrossBuildScalaVersion", "2.10.4")
      libraryDependencies.value
  }
}

publishTo <<= version {
  (v: String) =>
    val nexus = "https://oss.sonatype.org/"
    if (v.trim.endsWith("SNAPSHOT"))
      Some(Resolver.file("file", new File(Path.userHome.absolutePath + "/.m2/repository")))
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishMavenStyle := true

publishArtifact in Test := false

parallelExecution in Test := false

pomIncludeRepository := {
  _ => false
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