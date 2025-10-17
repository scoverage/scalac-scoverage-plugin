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

import buildinfo.BuildInfo
import scoverage.reporter.IOUtils

private[scoverage] object ScoverageCompiler {

  val ScalaVersion: String = BuildInfo.scalaVersion
  val ShortScalaVersion: String = (ScalaVersion split "[.]").toList match {
    case init :+ last if last forall (_.isDigit) => init mkString "."
    case _                                       => ScalaVersion
  }

  def classPath: Seq[String] =
    getScalaJars.map(
      _.getAbsolutePath
    ) :+ sbtCompileDir.getAbsolutePath :+ runtimeClasses("jvm").getAbsolutePath

  def jsClassPath: Seq[String] =
    getScalaJsJars.map(
      _.getAbsolutePath
    ) :+ sbtCompileDir.getAbsolutePath :+ runtimeClasses("js").getAbsolutePath

  def settings: Settings = settings(classPath)

  def jsSettings: Settings = {
    val s = settings(jsClassPath)
    s.plugin.value = List(getScalaJsCompilerJar.getAbsolutePath)
    s
  }

  def settings(classPath: Seq[String]): Settings = {
    val s = new scala.tools.nsc.Settings
    s.Xprint.value = List("all", "_")
    s.deprecation.value = true
    s.Yrangepos.value = true
    s.Yposdebug.value = true
    s.classpath.value = classPath.mkString(File.pathSeparator)

    val path =
      s"./plugin/target/scala-$ScalaVersion/test-generated-classes"
    new File(path).mkdirs()
    s.outdir.value = path
    s
  }

  def default: ScoverageCompiler = {
    val reporter = new scala.tools.nsc.reporters.ConsoleReporter(settings)
    new ScoverageCompiler(settings, reporter, validatePositions = true)
  }

  def noPositionValidation: ScoverageCompiler = {
    val reporter = new scala.tools.nsc.reporters.ConsoleReporter(settings)
    new ScoverageCompiler(settings, reporter, validatePositions = false)
  }

  def defaultJS: ScoverageCompiler = {
    val reporter = new scala.tools.nsc.reporters.ConsoleReporter(jsSettings)
    new ScoverageCompiler(jsSettings, reporter, validatePositions = true)
  }

  def locationCompiler: LocationCompiler = {
    val reporter = new scala.tools.nsc.reporters.ConsoleReporter(settings)
    new LocationCompiler(settings, reporter)
  }

  private def getScalaJars: List[File] = {
    val scalaJars = List("scala-compiler", "scala-library", "scala-reflect")
    scalaJars.map(findScalaJar)
  }

  private def getScalaJsJars: List[File] =
    findJar(
      "org.scala-js",
      s"scalajs-library_$ShortScalaVersion",
      BuildInfo.scalaJSVersion
    ) :: getScalaJars

  private def getScalaJsCompilerJar: File = findJar(
    "org.scala-js",
    s"scalajs-compiler_$ScalaVersion",
    BuildInfo.scalaJSVersion
  )

  private def sbtCompileDir: File = {
    val dir = new File(
      s"./plugin/target/scala-$ScalaVersion/classes"
    )
    if (!dir.exists)
      throw new FileNotFoundException(
        s"Could not locate SBT compile directory for plugin files [$dir]"
      )
    dir
  }

  private def runtimeClasses(platform: String): File = new File(
    s"./runtime/$platform/target/scala-$ScalaVersion/classes"
  )

  private def findScalaJar(artifactId: String): File =
    findJar("org.scala-lang", artifactId, ScalaVersion)

  private def findJar(
      groupId: String,
      artifactId: String,
      version: String
  ): File =
    findIvyJar(groupId, artifactId, version)
      .orElse(findCoursierJar(groupId, artifactId, version))
      .getOrElse {
        throw new FileNotFoundException(
          s"Could not locate $groupId:$artifactId:$version"
        )
      }

  private def findCoursierJar(
      groupId: String,
      artifactId: String,
      version: String
  ): Option[File] = {
    val userHome = System.getProperty("user.home")
    val jarPaths = Iterator(
      ".cache/coursier", // Linux
      "Library/Caches/Coursier", // MacOSX
      "AppData/Local/Coursier/cache" // Windows
    ).map { loc =>
      val gid = groupId.replace('.', '/')
      s"$userHome/$loc/v1/https/repo1.maven.org/maven2/$gid/$artifactId/$version/$artifactId-$version.jar"
    }
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
    rep: scala.tools.nsc.reporters.Reporter,
    validatePositions: Boolean
) extends scala.tools.nsc.Global(settings, rep) {

  def addToClassPath(file: File): Unit = {
    settings.classpath.value =
      settings.classpath.value + File.pathSeparator + file.getAbsolutePath
  }

  val instrumentationComponent =
    new ScoverageInstrumentationComponent(this, None, None)

  val coverageOptions = ScoverageOptions
    .default()
    .copy(dataDir = IOUtils.getTempPath)
    .copy(sourceRoot = IOUtils.getTempPath)

  instrumentationComponent.setOptions(coverageOptions)
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
    IOUtils.writeToFile(file, code, None)
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
    if (validatePositions)
      addToPhasesSet(validator, "scoverage validator")
    addToPhasesSet(
      instrumentationComponent,
      "scoverage instrumentationComponent"
    )
    addToPhasesSet(testStore, "scoverage teststore")
  }
}
