import sbt._

class ScctBuild(info: ProjectInfo) extends ParentProject(info) with IdeaPlugin {
	override def managedStyle = ManagedStyle.Maven
	lazy val publishTo = Resolver.file("github-pages-repo", new java.io.File("./maven-repo/"))

  lazy val scct = project("scct", "scct", new DefaultProject(_) with IdeaPlugin { //with reaktor.scct.ScctProject {
    //override def coverageSelfTest = true
    override def filterScalaJars = false
    val junit = "junit" % "junit" % "4.7" % "test" withSources
    val scalaTest = "org.scalatest" % "scalatest" % "1.0" % "test" withSources
    val mockito = "org.mockito" % "mockito-all" % "1.8.0" % "test" withSources
    val scalaCheck = "org.scala-tools.testing" % "scalacheck" % "1.6" % "test" withSources
    val scalaSpecs = "org.scala-tools.testing" % "specs" % "1.6.0" % "test" withSources
    val scalaCompiler = "org.scala-lang" % "scala-compiler" % "2.7.7" % "test"
  })

  lazy val sbtPlugin = project("sbt-scct", "sbt-scct", new PluginProject(_) with IdeaPlugin, scct)

}
