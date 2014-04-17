package scoverage

import java.io.{FileNotFoundException, File}
import scala.tools.nsc.reporters.Reporter

/** @author Stephen Samuel */
trait PluginSupport {

  val scalaVersion = "2.10.3"
  val shortScalaVersion = "2.10"

  val settings = new scala.tools.nsc.Settings
  val classPath = getScalaJars.map(_.getAbsolutePath) :+ sbtCompileDir.getAbsolutePath
  settings.Xprint.value = List("all")
  settings.classpath.value = classPath.mkString(":")

  val reporter = new scala.tools.nsc.reporters.ConsoleReporter(settings)

  def writeCodeSnippetToTempFile(code: String): File = {
    val file = File.createTempFile("scoverage_snippet", ".scala")
    org.apache.commons.io.FileUtils.write(file, code)
    file
  }

  def compileCodeSnippet(code: String): SimpleCompiler = compileSourceFiles(writeCodeSnippetToTempFile(code))

  def compileSourceFiles(files: File*): SimpleCompiler = {
    val compiler = new SimpleCompiler(settings, reporter)
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

  def sbtCompileDir: File = {
    val dir = new File("./target/scala-" + shortScalaVersion + "/classes")
    if (dir.exists) dir
    else throw new FileNotFoundException(s"Could not locate SBT compile directory for plugin files [$dir]")
  }

}

class SimpleCompiler(settings: scala.tools.nsc.Settings, reporter: scala.tools.nsc.reporters.Reporter)
  extends scala.tools.nsc.Global(settings, reporter) {
  val scoverageComponent = new ScoverageComponent(this)
  scoverageComponent.setOptions(new ScoverageOptions())
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


