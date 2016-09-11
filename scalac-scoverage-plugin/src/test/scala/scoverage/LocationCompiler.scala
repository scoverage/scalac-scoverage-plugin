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
    override val runsRightAfter    = Some("typer")
    override val runsAfter         = List("typer")
    override val runsBefore        = List[String]("patmat")

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
    super.computeInternalPhases()
    addToPhasesSet(locationSetter, "sets locations")
  }
}
