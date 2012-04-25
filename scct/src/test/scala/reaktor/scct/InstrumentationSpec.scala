package reaktor.scct

import scala.tools.nsc.reporters.ConsoleReporter
import tools.nsc.{SubComponent, Global, CompilerCommand, Settings}
import java.io.{File, FileOutputStream}
import org.specs.matcher.Matcher
import org.specs.Specification

trait InstrumentationSpec extends Specification with InstrumentationSupport {
  def instrument = addToSusVerb("instrument")

  def classOffsetsMatch(s: String) {
    offsetsMatch("class Foo@(x: Int) {\n  "+s+"\n}")
  }
  def defOffsetsMatch(s: String) {
    offsetsMatch("class Foo@(x: Int) {\n  def foo {\n    "+s+"\n  }\n}")
  }
  def offsetsMatch(s: String) {
    offsetsMatch(parse(0, s, InstrumentationSpec("", Nil)), false)
  }
  def placeHoldersMatch(s: String) {
    offsetsMatch(parse(0, s, InstrumentationSpec("", Nil)), true)
  }
  def offsetsMatch(spec: InstrumentationSpec, placeHoldersOnly: Boolean) {
    val resultOffsets = compileToData(spec.source).filter(x => placeHoldersOnly == x.placeHolder).map(_.offset)
    resultOffsets must matchSpec(spec)
  }
  def compileSource(source: String) = {
    compile(source)
  }

}

trait InstrumentationSupport {
  def scalaVersion = "2.9.1"
  def debug = false

  def compileFile(file: String) = compileFiles(Seq(file) :_*)
  def compileFiles(args: String*) = {
    val settings = createSettings
    val command = new CompilerCommand(args.toList, settings)
    val runner = new PluginRunner(settings, debug)
    try {
      (new runner.Run).compile(command.files)
    } catch {
      case e:scala.tools.nsc.MissingRequirementError => {
        println("test-fail: Scala compiler is probably not finding scala jars (catch-22).\nFix paths in InstrumentationSpec:createSettings.")
        throw e
      }
      case e:Exception => throw e
    }
    runner
  }

  def createSettings = {
    val settings = new Settings
    val scalaJars = List("scala-compiler.jar", "scala-library.jar")
    val classPath = if (TestEnv.isSbt) {
      "./target/scala_"+scalaVersion+"/classes" :: scalaJars.map("./project/boot/scala-"+scalaVersion+"/lib/"+_)
    } else {
      // Assume IntelliJ IDEA with working dir as project root (ie. $git/):
      "out/production/scct" :: scalaJars.map("./scct/project/boot/scala-"+scalaVersion+"/lib/"+_)
      // IDEA keeps changing default working dir btw. module and project root btw. versions, last time it was this:
      // "../out/production/scct" :: scalaJars.map("./project/boot/scala-"+scalaVersion+"/lib/"+_)
    }
    settings.classpath.value = classPath.mkString(":")
    settings
  }

  def compile(line: String): PluginRunner = {
    Some(line).map(writeFile).map(compileFile).get
  }
  def compileToData(line: String): List[CoveredBlock] = {
    Some(compile(line)).map(_.scctComponent.data).map(sort).get
  }

  def writeFile(line: String): String = {
    val f = File.createTempFile("scct-test-compiler", ".scala")
    IO.withOutputStream(new FileOutputStream(f)) { out => out.write(line.getBytes("utf-8")) }
    f.getCanonicalPath
  }

  def sort(data: List[CoveredBlock]) = data.sortWith { (a,b) =>
    if (a.name.sourceFile == b.name.sourceFile) {
      a.offset < b.offset
    } else a.name.sourceFile < b.name.sourceFile
  }

  def parse(current: Int, s: String, acc: InstrumentationSpec): InstrumentationSpec = {
    val (curr, next) = splitAtMark(s)
    if (next.length > 0) {
      val newAcc = acc + (curr, current + curr.length)
      parse(current + curr.length, next, newAcc)
    } else {
      acc + curr
    }
  }

  private def splitAtMark(s: String): Tuple2[String,String] = {
    var isEscape = false
    val idx = s.indexWhere { _ match {
      case '@' if !isEscape => true
      case '\\' => { isEscape = true; false }
      case _ => { isEscape = false; false }
    }}
    if (idx >= 0)
      (s.substring(0, idx).replace("\\@", "@"), s.substring(idx + 1))
    else
      (s.replace("\\@", "@"), "")
  }

  case class matchSpec(spec: InstrumentationSpec) extends Matcher[List[Int]]() {
    def toS(l: Seq[Int]) = l.mkString("[",",","]")
    def apply(v: => List[Int]) = (v == spec.expectedOffsets, "ok",
            "Offset mismatch: %s != %s\n\n%s\n - doesn't match expected - \n\n%s".format(toS(v), toS(spec.expectedOffsets), printOffsets(v, spec.source), printOffsets(spec.expectedOffsets, spec.source)))
  }

  case class InstrumentationSpec(source: String, expectedOffsets: List[Int]) {
    def +(s: String): InstrumentationSpec = InstrumentationSpec(source + s, expectedOffsets)
    def +(s: String, o: Int): InstrumentationSpec = InstrumentationSpec(source + s, expectedOffsets ::: List(o))
  }

  val displayedTabWidth = 2
  val tab = 1.to(displayedTabWidth).map(_ => " ").mkString

  def printOffsets(offsets: Iterable[Int], compilationUnit: String): String = {
    printOffsets(0, offsets, compilationUnit.split("\n").toList)
  }
  private def printOffsets(offset: Int, data: Iterable[Int], compilationUnit: List[String]): String = {
    compilationUnit match {
      case Nil => ""
      case line :: tail => {
        val maxOffset = offset + line.length
        val (currData, nextData) = data.partition(_ < maxOffset)
        val blocks = blockLine(line, offset, currData)
        line.replaceAll("\t", tab) + blocks.map("\n"+_).getOrElse("") + "\n" + printOffsets(maxOffset + 1, nextData, tail)
      }
    }
  }
  private def blockLine(line: String, offset: Int, data: Iterable[Int]): Option[String] = data match {
    case Nil => None
    case d => Some(blockIndicators(calculateTabbedOffsets(line.toList, data.map(_ - offset).toList)) + " ("+d.mkString(":")+")")
  }
  private def calculateTabbedOffsets(line: List[Char], data: List[Int]): List[Int] = data match {
    case Nil => Nil
    case offset :: tail => {
      val (pre, post) = line.splitAt(offset)
      val numTabs = pre.count(_ == '\t')
      (offset + (numTabs*(displayedTabWidth-1))) :: calculateTabbedOffsets(post, tail)
    }
  }
  private def blockIndicators(columns: List[Int]) = columns match {
    case Nil => ""
    case _ => {
      val s = 0.to(columns.last+1).map(idx => " ").mkString
      columns.foldLeft(s) { (result, idx) => result.substring(0,idx)+"^"+result.substring(idx+1) }
    }
  }
}

class PluginRunner(settings: Settings, debug: Boolean) extends Global(settings, new ConsoleReporter(settings)) {
  lazy val scctComponent = {
    val scctTransformer = new ScctTransformComponent(this)
    scctTransformer.debug = debug
    scctTransformer.saveData = false
    scctTransformer
  }
  override def computeInternalPhases() {
    phasesSet += syntaxAnalyzer
    phasesSet += analyzer.namerFactory
    phasesSet += analyzer.packageObjects
    phasesSet += analyzer.typerFactory
    phasesSet += superAccessors
    phasesSet += pickler
    phasesSet += refchecks
    phasesSet += scctComponent
  }
}