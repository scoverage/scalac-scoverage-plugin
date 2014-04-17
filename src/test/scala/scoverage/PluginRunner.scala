package scoverage

import java.io.{FileNotFoundException, File}

/** @author Stephen Samuel */
trait PluginSupport {

  val scalaVersion = "2.10.3"
  val shortScalaVersion = "2.10"

  val settings = new scala.tools.nsc.Settings
  val classPath = getScalaJars.map(_.getAbsolutePath) :+ sbtCompileDir.getAbsolutePath
  settings.Xprint.value = List("all")
  settings.classpath.value = classPath.mkString(":")

  val reporter = new scala.tools.nsc.reporters.ConsoleReporter(settings)

  val compiler = new ScoverageAwareCompiler(settings, reporter)

  def writeCodeSnippetToTempFile(code: String): File = {
    val file = File.createTempFile("scoverage_snippet", ".scala")
    org.apache.commons.io.FileUtils.write(file, code)
    file
  }

  def compileCodeSnippet(code: String): ScoverageAwareCompiler = compileSourceFiles(writeCodeSnippetToTempFile(code))

  def compileSourceFiles(files: File*): ScoverageAwareCompiler = {
    val command = new scala.tools.nsc.CompilerCommand(files.map(_.getAbsolutePath).toList, settings)
    new compiler.Run().compile(command.files)
    compiler
  }

  def getScalaJars: List[File] = {
    val scalaJars = List("scala-compiler.jar", "scala-library.jar", "scala-reflect.jar")
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

class ScoverageAwareCompiler(settings: scala.tools.nsc.Settings, reporter: scala.tools.nsc.reporters.Reporter)
  extends scala.tools.nsc.Global(settings, reporter) {
  val scoverageComponent = new ScoverageComponent(this)
  scoverageComponent.setOptions(new ScoverageOptions())
  override def computeInternalPhases() {
    val phs = List(
      syntaxAnalyzer -> "parse source into ASTs, perform simple desugaring",
      analyzer.namerFactory -> "resolve names, attach symbols to named trees",
      analyzer.packageObjects -> "load package objects",
      analyzer.typerFactory -> "the meat and potatoes: type the trees",
      scoverageComponent -> "scoverage",
      patmat -> "translate match expressions",
      superAccessors -> "add super accessors in traits and nested classes",
      extensionMethods -> "add extension methods for inline classes",
      pickler -> "serialize symbol tables",
      refChecks -> "reference/override checking, translate nested objects",
      uncurry -> "uncurry, translate function values to anonymous classes",
      tailCalls -> "replace tail calls by jumps",
      specializeTypes -> "@specialized-driven class and method specialization",
      explicitOuter -> "this refs to outer pointers, translate patterns",
      erasure -> "erase types, add interfaces for traits",
      postErasure -> "clean up erased inline classes",
      lazyVals -> "allocate bitmaps, translate lazy vals into lazified defs",
      lambdaLift -> "move nested functions to top level",
      constructors -> "move field definitions into constructors",
      mixer -> "mixin composition",
      cleanup -> "platform-specific cleanups, generate reflective calls",
      genicode -> "generate portable intermediate code",
      inliner -> "optimization: do inlining",
      inlineExceptionHandlers -> "optimization: inline exception handlers",
      closureElimination -> "optimization: eliminate uncalled closures",
      deadCode -> "optimization: eliminate dead code",
      terminal -> "The last phase in the compiler chain"
    )
    phs foreach (addToPhasesSet _).tupled
  }
}


