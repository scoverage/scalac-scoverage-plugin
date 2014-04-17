package scoverage

import java.io.{FileNotFoundException, File}

/** @author Stephen Samuel */
trait PluginSupport {

  val scalaVersion = "2.10.3"

  def writeCodeSnippetToTempFile(code: String): File = {
    val file = File.createTempFile("scoverage_snippet", ".scala")
    org.apache.commons.io.FileUtils.write(file, code)
    file
  }

  def compileCodeSnippet(code: String): SimpleCompiler = compileSourceFiles(writeCodeSnippetToTempFile(code))

  def compileSourceFiles(files: File*): SimpleCompiler = {
    val settings = createSettings
    val compiler = new SimpleCompiler(settings)
    val command = new scala.tools.nsc.CompilerCommand(files.map(_.getAbsolutePath).toList, settings)
    new compiler.Run().compile(command.files)
    compiler
  }

  def getScalaJars: List[File] = {
    val scalaJars = List("scala-compiler.jar", "scala-library.jar")
    scalaJars.map(findScalaJar)
  }

  def findScalaJar(jarName: String): File = {
    val userHome = System.getProperty("user.home")
    val sbtHome = userHome + "/.sbt"
    val sbtScalaLibs = sbtHome + "/boot/scala-" + scalaVersion + "/lib"
    val file = new File(sbtScalaLibs + "/" + jarName)
    if (file.exists) file else throw new FileNotFoundException(s"Could not locate [$jarName]. Tests require SBT 0.13+")
  }

  def createSettings = {
    val settings = new scala.tools.nsc.Settings
    val classPath = getScalaJars.map(_.getAbsolutePath)
    settings.Xprint.value = List("all")
    settings.classpath.value = classPath.mkString(":")
    settings
  }
}

class SimpleCompiler(settings: scala.tools.nsc.Settings)
  extends scala.tools.nsc.Global(settings, new scala.tools.nsc.reporters.ConsoleReporter(settings)) {
  lazy val scoverageComponent = new ScoverageComponent(this)
  override def computeInternalPhases() {
    val phs = List(
      syntaxAnalyzer -> "parse source into ASTs, perform simple desugaring",
      analyzer.namerFactory -> "resolve names, attach symbols to named trees",
      analyzer.packageObjects -> "load package objects",
      analyzer.typerFactory -> "the meat and potatoes: type the trees",
      scoverageComponent -> "scoverage"
    )
    phs foreach (addToPhasesSet _).tupled
  }
}


