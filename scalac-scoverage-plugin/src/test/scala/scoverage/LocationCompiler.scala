package scoverage

import java.io.File

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.{Transform, TypingTransformers}

class LocationCompiler(settings: scala.tools.nsc.Settings, reporter: scala.tools.nsc.reporters.Reporter)
  extends scala.tools.nsc.Global(settings, reporter) {

  val locations = List.newBuilder[(String, Location)]
  private val locationSetter = new LocationSetter(this)

  def compile(code: String): Unit = {
    val files = writeCodeSnippetToTempFile(code)
    val command = new scala.tools.nsc.CompilerCommand(List(files.getAbsolutePath), settings)
    new Run().compile(command.files)
  }

  def writeCodeSnippetToTempFile(code: String): File = {
    val file = File.createTempFile("code_snippet", ".scala")
    IOUtils.writeToFile(file, code)
    file.deleteOnExit()
    file
  }

  class LocationSetter(val global: Global) extends PluginComponent with TypingTransformers with Transform {

    override val phaseName: String = "location-setter"
    override val runsAfter: List[String] = List("typer")
    override val runsBefore = List[String]("patmat")

    override protected def newTransformer(unit: global.CompilationUnit): global.Transformer = new Transformer(unit)
    class Transformer(unit: global.CompilationUnit) extends TypingTransformer(unit) {

      override def transform(tree: global.Tree) = {
        for ( location <- Location(global)(tree) ) {
          locations += (tree.getClass.getSimpleName -> location)
        }
        super.transform(tree)
      }
    }
  }

  override def computeInternalPhases() {
    val phs = List(
      syntaxAnalyzer -> "parse source into ASTs, perform simple desugaring",
      analyzer.namerFactory -> "resolve names, attach symbols to named trees",
      analyzer.packageObjects -> "load package objects",
      analyzer.typerFactory -> "the meat and potatoes: type the trees",
      locationSetter -> "sets locations",
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