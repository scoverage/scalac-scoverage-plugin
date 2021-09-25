package scoverage

import java.io.File
import java.io.FileNotFoundException
import java.net.URL

import scala.collection.mutable.ListBuffer
import scala.tools.nsc.Global
import scala.tools.nsc.Settings
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.Transform
import scala.tools.nsc.transform.TypingTransformers

object ScoverageCompiler {

  val ScalaVersion: String = scala.util.Properties.versionNumberString
  val ShortScalaVersion: String = (ScalaVersion split "[.]").toList match {
    case init :+ last if last forall (_.isDigit) => init mkString "."
    case _                                       => ScalaVersion
  }

  def classPath: Seq[String] =
    getScalaJars.map(
      _.getAbsolutePath
    ) :+ sbtCompileDir.getAbsolutePath :+ runtimeClasses.getAbsolutePath

  def settings: Settings = {
    val s = new scala.tools.nsc.Settings
    s.Xprint.value = List("all", "_")
    s.deprecation.value = true
    s.Yrangepos.value = true
    s.Yposdebug.value = true
    s.classpath.value = classPath.mkString(File.pathSeparator)

    val path =
      s"./scalac-scoverage-plugin/target/scala-$ScalaVersion/test-generated-classes"
    new File(path).mkdirs()
    s.outdir.value = path
    s
  }

  def default: ScoverageCompiler = {
    val reporter = new scala.tools.nsc.reporters.ConsoleReporter(settings)
    new ScoverageCompiler(settings, reporter)
  }

  def locationCompiler: LocationCompiler = {
    val reporter = new scala.tools.nsc.reporters.ConsoleReporter(settings)
    new LocationCompiler(settings, reporter)
  }

  private def getScalaJars: List[File] = {
    val scalaJars = List("scala-compiler", "scala-library", "scala-reflect")
    scalaJars.map(findScalaJar)
  }

  private def sbtCompileDir: File = {
    val dir = new File(
      s"./scalac-scoverage-plugin/target/scala-$ScalaVersion/classes"
    )
    if (!dir.exists)
      throw new FileNotFoundException(
        s"Could not locate SBT compile directory for plugin files [$dir]"
      )
    dir
  }

  private def runtimeClasses: File = new File(
    s"./scalac-scoverage-runtime/jvm/target/scala-$ScalaVersion/classes"
  )

  private def findScalaJar(artifactId: String): File =
    findIvyJar("org.scala-lang", artifactId, ScalaVersion)
      .orElse(findCoursierJar(artifactId, ScalaVersion))
      .getOrElse {
        throw new FileNotFoundException(
          s"Could not locate $artifactId/$ScalaVersion"
        )
      }

  private def findCoursierJar(
      artifactId: String,
      version: String
  ): Option[File] = {
    val userHome = System.getProperty("user.home")
    val jarPaths = Iterator(
      ".cache/coursier", // Linux
      "Library/Caches/Coursier", // MacOSX
      "AppData/Local/Coursier/cache" // Windows
    ).map(loc =>
      s"$userHome/$loc/v1/https/repo1.maven.org/maven2/org/scala-lang/$artifactId/$version/$artifactId-$version.jar"
    )
    jarPaths.map(new File(_)).find(_.exists())
  }

  private def findIvyJar(
      groupId: String,
      artifactId: String,
      version: String,
      packaging: String = "jar"
  ): Option[File] = {
    val userHome = System.getProperty("user.home")
    val jarPath =
      s"$userHome/.ivy2/cache/$groupId/$artifactId/${packaging}s/$artifactId-$version.jar"
    val file = new File(jarPath)
    if (file.exists()) Some(file) else None
  }
}

class ScoverageCompiler(
    settings: scala.tools.nsc.Settings,
    rep: scala.tools.nsc.reporters.Reporter
) extends scala.tools.nsc.Global(settings, rep) {

  def addToClassPath(file: File): Unit = {
    settings.classpath.value =
      settings.classpath.value + File.pathSeparator + file.getAbsolutePath
  }

  val instrumentationComponent =
    new ScoverageInstrumentationComponent(this, None, None)

  instrumentationComponent.setOptions(new ScoverageOptions())
  val testStore = new ScoverageTestStoreComponent(this)
  val validator = new PositionValidator(this)

  def compileSourceFiles(files: File*): ScoverageCompiler = {
    val command = new scala.tools.nsc.CompilerCommand(
      files.map(_.getAbsolutePath).toList,
      settings
    )
    new Run().compile(command.files)
    this
  }

  def writeCodeSnippetToTempFile(code: String): File = {
    val file = File.createTempFile("scoverage_snippet", ".scala")
    IOUtils.writeToFile(file, code)
    file.deleteOnExit()
    file
  }

  def compileCodeSnippet(code: String): ScoverageCompiler = compileSourceFiles(
    writeCodeSnippetToTempFile(code)
  )
  def compileSourceResources(urls: URL*): ScoverageCompiler = {
    compileSourceFiles(urls.map(_.getFile).map(new File(_)): _*)
  }

  def assertNoErrors() =
    assert(!reporter.hasErrors, "There are compilation errors")

  def assertNoCoverage() = assert(
    !testStore.sources.mkString(" ").contains(s"scoverage.Invoker.invoked"),
    "There are scoverage.Invoker.invoked instructions added to the code"
  )

  def assertNMeasuredStatements(n: Int): Unit = {
    for (k <- 1 to n) {
      assert(
        testStore.sources
          .mkString(" ")
          .contains(s"scoverage.Invoker.invoked($k,"),
        s"Should be $n invoked statements but missing #$k"
      )
    }
    assert(
      !testStore.sources
        .mkString(" ")
        .contains(s"scoverage.Invoker.invoked(${n + 1},"),
      s"Found statement ${n + 1} but only expected $n"
    )
  }

  class PositionValidator(val global: Global)
      extends PluginComponent
      with TypingTransformers
      with Transform {

    override val phaseName = "scoverage-validator"
    override val runsAfter = List("typer")
    override val runsBefore = List("scoverage-instrumentation")

    override protected def newTransformer(
        unit: global.CompilationUnit
    ): global.Transformer = new Transformer(unit)
    class Transformer(unit: global.CompilationUnit)
        extends TypingTransformer(unit) {

      override def transform(tree: global.Tree) = {
        global.validatePositions(tree)
        tree
      }
    }
  }

  class ScoverageTestStoreComponent(val global: Global)
      extends PluginComponent
      with TypingTransformers
      with Transform {

    val sources = new ListBuffer[String]

    override val phaseName = "scoverage-teststore"
    override val runsAfter = List("jvm")
    override val runsBefore = List("terminal")

    override protected def newTransformer(
        unit: global.CompilationUnit
    ): global.Transformer = new Transformer(unit)
    class Transformer(unit: global.CompilationUnit)
        extends TypingTransformer(unit) {

      override def transform(tree: global.Tree) = {
        sources += tree.toString
        tree
      }
    }
  }

  override def computeInternalPhases(): Unit = {
    super.computeInternalPhases()
    addToPhasesSet(validator, "scoverage validator")
    addToPhasesSet(
      instrumentationComponent,
      "scoverage instrumentationComponent"
    )
    addToPhasesSet(testStore, "scoverage teststore")
  }
}
