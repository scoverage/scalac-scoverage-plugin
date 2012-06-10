import sbt._
import sbt.Keys._

object SimpleTestBuild extends Build {
  override lazy val projects = Seq(root)
  lazy val root = Project("root", file(".")) settings(ScctPlugin.instrumentSettings : _*)
}