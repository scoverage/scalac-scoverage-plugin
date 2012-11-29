package reaktor.scct

import scala.tools.nsc.reporters.ConsoleReporter
import java.io.{FileNotFoundException, File, FileOutputStream}
import org.specs2.matcher.{Expectable, Matcher}
import org.specs2.mutable._
import tools.nsc._

trait InstrumentationSpec extends Specification with InstrumentationSupport {

  def classOffsetsMatch(s: String) = {
    offsetsMatch("class Foo@(x: Int) {\n  "+s+"\n}")
  }
  def defOffsetsMatch(s: String) = {
    offsetsMatch("class Foo@(x: Int) {\n  def foo {\n    "+s+"\n  }\n}")
  }
  def offsetsMatch(s: String) = {
    _offsetsMatch(parse(0, s, InstrumentationSpec("", Nil)), false)
  }
  def placeHoldersMatch(s: String) = {
    _offsetsMatch(parse(0, s, InstrumentationSpec("", Nil)), true)
  }
  def _offsetsMatch(spec: InstrumentationSpec, placeHoldersOnly: Boolean) = {
    val resultOffsets = compileToData(spec.source).filter(x => placeHoldersOnly == x.placeHolder).map(_.offset)
    resultOffsets must matchSpec(spec)
  }
  def compileSource(source: String) = {
    compile(source)
  }

}

trait InstrumentationSupport {
  def scalaVersion = System.getProperty("scct-test-scala-version", "2.10.0-RC3")
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
    val classPath = locateCompiledClasses() :: locateScalaJars()
    //settings.Xprint.value = List("all")
    settings.classpath.value = classPath.mkString(":")
    settings
  }

  def locateCompiledClasses() = {
    val scalaTargetDir = scalaVersion match {
      case "2.10.0-RC3" => "2.10"
      case x => x
    }
    val first = new File("./target/scala-"+scalaTargetDir+"/classes")
    val second = new File("./scct/target/scala-"+scalaTargetDir+"/classes")
    if (first.exists) {
      // sbt || IDEA with module dir as working dir
      "./target/scala-"+scalaTargetDir+"/classes"
    } else if (second.exists) {
      // IDEA, with project dir as working dir
      "./scct/target/scala-"+scalaTargetDir+"/classes"
    } else {
      val err = "Compiled classes not found. Looked in " + first.getAbsolutePath + " and " + second.getAbsolutePath
      throw new FileNotFoundException(err+ " Check InstrumentationSpec:locateCompiledClasses")
    }
  }

  def locateScalaJars() = {
    val scalaJars = List("scala-compiler.jar", "scala-library.jar")
    val userHome = System.getProperty("user.home")
    if (new File(System.getProperty("user.home") + "/.sbt/boot/scala-"+scalaVersion+"/lib/"+ scalaJars.head).exists) {
      // sbt 0.11+ with global boot dirs
      scalaJars.map(userHome + "/.sbt/boot/scala-"+scalaVersion+"/lib/"+_)
    } else if (new File("./project/boot/scala-"+scalaVersion+"/lib/" + scalaJars.head).exists) {
      // sbt 0.7.7 and such, project-specific boot dirs
      scalaJars.map("./project/boot/scala-"+scalaVersion+"/lib/"+_)
    } else if (new File("./scct/project/boot/scala-"+scalaVersion+"/lib/" + scalaJars.head).exists) {
      // Probably IDEA with project dir instead of module dir as working dir
      scalaJars.map("./scct/project/boot/scala-"+scalaVersion+"/lib/"+_)
    } else {
      throw new FileNotFoundException("scala jars not found. Check InstrumentationSpec:locateScalaJars")
    }
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

    def apply[S <: List[Int]](s: Expectable[S]) = result(
      s.value == spec.expectedOffsets,
      "ok",
      "Offset mismatch: %s != %s\n\n%s\n - doesn't match expected - \n\n%s".format(
        toS(s.value), toS(spec.expectedOffsets),
        printOffsets(s.value, spec.source), printOffsets(spec.expectedOffsets, spec.source)
      ),
      s
    )
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
    val opts = new ScctInstrumentPluginOptions("compilationId", "default", new File(System.getProperty("user.dir", ".")))
    val scctTransformer = new ScctTransformComponent(this, opts)
    scctTransformer.debug = debug
    scctTransformer.saveData = false
    scctTransformer
  }
  override def computeInternalPhases() {
    val phs = List(
      syntaxAnalyzer          -> "parse source into ASTs, perform simple desugaring",
      analyzer.namerFactory   -> "resolve names, attach symbols to named trees",
      analyzer.packageObjects -> "load package objects",
      analyzer.typerFactory   -> "the meat and potatoes: type the trees",
      //patmat                  -> "translate match expressions",
      //superAccessors          -> "add super accessors in traits and nested classes",
      //extensionMethods        -> "add extension methods for inline classes",
      //pickler                 -> "serialize symbol tables",
      //refChecks               -> "reference/override checking, translate nested objects",
      scctComponent           -> "That's me!"
    )
    phs foreach (addToPhasesSet _).tupled
  }
}