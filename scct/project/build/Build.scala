import sbt._
import java.util.jar.Manifest

class Build(info: ProjectInfo) extends DefaultProject(info) with IdeaProject {

	//override def managedStyle = ManagedStyle.Maven
	//lazy val publishTo = Resolver.file("github-pages-repo", new java.io.File("../maven-repo/"))

  val junit = "junit" % "junit" % "4.7" % "test" withSources
  val mockito = "org.mockito" % "mockito-all" % "1.8.5" % "test" withSources
  val scalaSpecs = "org.scala-tools.testing" % "specs_2.8.0" % "1.6.5" % "test" withSources

  // Self-test

  def scctPluginJar = jarPath
  def instrumentedClassDir = outputPath / "coverage-classes"
  def reportDir = outputPath / "coverage-report"
  def selfTestRunClasspath = instrumentedClassDir +++ (testClasspath --- mainCompilePath)

  class InstrumentCompileConfig extends MainCompileConfig {
    override def label = "coverage"
    override def outputDirectory = instrumentedClassDir
    override def analysisPath = outputPath / "coverage-analysis"
    override def classpath = scctPluginJar +++ (super.classpath --- mainCompilePath)
    override def baseCompileOptions = coverageCompileOption :: super.baseCompileOptions.toList
    def coverageCompileOption = CompileOption("-Xplugin:"+scctPluginJar.get.mkString)
  }

  lazy val setupCoverageEnv = task {
    FileUtilities.clean(reportDir, log)
    FileUtilities.createDirectory(reportDir, log)
    System.setProperty("scct.report.dir", reportDir.toString)
    System.setProperty("scct.src.reference.dir", mainScalaSourcePath.absolutePath)
    None
  }

  lazy val instrumentCompileConditional = new CompileConditional(new InstrumentCompileConfig, buildCompiler)
  lazy val instrument = task { instrumentCompileConditional.run } dependsOn `package`
  lazy val coverage = testTask(testFrameworks, selfTestRunClasspath, testCompileConditional.analysis, testOptions) dependsOn(instrument, testCompile, copyResources, copyTestResources, setupCoverageEnv)
}
