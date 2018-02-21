package scoverage

import java.io.{File, FileNotFoundException}
import java.net.URL

import scala.collection.mutable.ListBuffer
import scala.tools.nsc.{Settings, Global}
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.{Transform, TypingTransformers}

trait ScalaLoggingSupport {

  val scalaLoggingPackageName: String = if (ScoverageCompiler.ShortScalaVersion == "2.10") {
    "com.typesafe.scalalogging.slf4j"
  }
  else {
    "com.typesafe.scalalogging"
  }

  lazy val scalaLoggingDeps: Seq[File] = {
    if (ScoverageCompiler.ShortScalaVersion == "2.10") {
      Seq(
        findIvyJar("org.slf4j", "slf4j-api", "1.7.7"),
        findCrossedIvyJar("com.typesafe.scala-logging", "scala-logging-api", "2.1.2"),
        findCrossedIvyJar("com.typesafe.scala-logging", "scala-logging-slf4j", "2.1.2")
      )
    }
    else {
      Seq(
        findIvyJar("org.slf4j", "slf4j-api", "1.7.25"),
        findCrossedIvyJar("com.typesafe.scala-logging", "scala-logging", "3.8.0", "bundle")
      )
    }
  }

  private def findCrossedIvyJar(groupId: String, artifactId: String, version: String, packaging: String = "jar"): File =
    findIvyJar(groupId, artifactId + "_" + ScoverageCompiler.ShortScalaVersion, version, packaging)

  private def findIvyJar(groupId: String, artifactId: String, version: String, packaging: String = "jar"): File = {
    val userHome = System.getProperty("user.home")
    val jarPath = s"$userHome/.ivy2/cache/$groupId/$artifactId/${packaging}s/${artifactId}-${version}.jar"
    val file = new File(jarPath)
    if (!file.exists)
      throw new FileNotFoundException(s"Could not locate [$jarPath].")
    file
  }

}
