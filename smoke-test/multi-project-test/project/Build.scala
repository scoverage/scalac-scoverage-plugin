import sbt._
import Keys._

object MultiProjectTestBuild extends Build {
    lazy val root = Project(id = "multi-project-test",
                            base = file(".")) aggregate(first, second, third)

    lazy val first = Project(id = "first", base = file("first"))
    // adding scct plugin settings to individual projects works, just append this: settings(ScctPlugin.scctSettings: _*)

    lazy val second = Project(id = "second", base = file("second"))
    lazy val third = Project(id = "third", base = file("third")) aggregate(grandchild)
    lazy val grandchild = Project(id = "grandchild", base = file("third/grand-child")) dependsOn(second)

    override lazy val settings = super.settings ++ Seq(
      organization := "reaktor.scct",
      scalaVersion := "2.9.1",
      libraryDependencies ++= Seq(
        "junit" % "junit" % "4.10" % "test",
        "org.specs2" %% "specs2" % "1.9" % "test"
      )
    ) //++ Seq(org.example.MyPlugin.myPluginSettings: _*) ++ Seq(org.example.MyPlugin.newSetting := "fuckin huoh eh")
    // Adding scct to parent project settings doesnt work, it cant see other required settings, setting it in children...
    // ++ seq(ScctPlugin.scctSettings: _*)
}