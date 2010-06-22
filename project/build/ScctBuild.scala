import sbt._

class ScctBuild(info: ProjectInfo) extends ParentProject(info) with IdeaProject {
	override def managedStyle = ManagedStyle.Maven
	lazy val publishTo = Resolver.file("github-pages-repo", new java.io.File("./maven-repo/"))

  lazy val scct = project("scct", "scct", new DefaultProject(_) with IdeaProject { //with reaktor.scct.ScctProject {
    //override def coverageSelfTest = true
    val junit = "junit" % "junit" % "4.7" % "test" withSources
    val mockito = "org.mockito" % "mockito-all" % "1.8.5" % "test" withSources
    val scalaSpecs = "org.scala-tools.testing" % "specs_2.8.0.RC1" % "1.6.5-SNAPSHOT" % "test" withSources
  })

  lazy val sbtPlugin = project("sbt-scct", "sbt-scct", new PluginProject(_) with IdeaProject, scct)

  val snapshots = ScalaToolsSnapshots
}
