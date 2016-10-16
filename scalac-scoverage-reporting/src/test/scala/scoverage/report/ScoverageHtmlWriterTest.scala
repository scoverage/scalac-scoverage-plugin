package scoverage

import java.io._
import java.util.UUID
import scala.io.Source

import scala.xml.XML

import scoverage.report.ScoverageHtmlWriter

import org.scalatest.FunSuite

class ScoverageHtmlWriterTest extends FunSuite {

  val rootDirForClasses = new File(getClass.getResource("forHtmlWriter/src/main/scala/").getFile)

  def pathToClassFile(classLocation: String): String =
    new File(rootDirForClasses, classLocation).getCanonicalPath

  val pathToClassContainingHtml = pathToClassFile("ClassContainingHtml.scala")
  val pathToClassInSubDir = pathToClassFile("subdir/ClassInSubDir.scala")
  val pathToClassInMainDir = pathToClassFile("ClassInMainDir.scala")

  val statementForClassContainingHtml = Statement(pathToClassContainingHtml,
    Location("coverage.sample", "ClassContainingHtml", "ClassContainingHtml", ClassType.Class, "some_html", pathToClassInSubDir),
    3, 74, 97, 4, "<div>HTML content</div>",
    "scala.Predef.println", "Apply", false, 0)
  val statementForClassInSubDir = Statement(pathToClassInSubDir,
    Location("coverage.sample", "ClassInSubDir", "ClassInSubDir", ClassType.Class, "msg_test", pathToClassInSubDir),
    2, 64, 84, 4, "scala.this.Predef.println(\"test code\")",
    "scala.Predef.println", "Apply", false, 0)
  val statementForClassInMainDir = Statement(pathToClassInMainDir,
    Location("coverage.sample", "ClassInMainDir", "ClassInMainDir", ClassType.Class, "msg_coverage", pathToClassInMainDir),
    1, 69, 104, 4, "scala.this.Predef.println(\"measure coverage of code\")",
    "scala.Predef.println", "Apply", false, 0)

  def createTemporaryDir(): File = {
    val dir = new File(IOUtils.getTempDirectory, UUID.randomUUID().toString)
    dir.mkdirs()
    dir.deleteOnExit()
    dir
  }

  def writeCoverageToTemporaryDir(coverage: Coverage): File = {
    val outputDir = createTemporaryDir()
    val htmlWriter = new ScoverageHtmlWriter(rootDirForClasses, outputDir)
    htmlWriter.write(coverage)
    outputDir
  }

  test("HTML coverage report contains correct links") {

    val coverage = Coverage()
    coverage.add(statementForClassInSubDir)
    coverage.add(statementForClassInMainDir)

    val outputDir = writeCoverageToTemporaryDir(coverage)

    val htmls = List("overview.html", "coverage.sample.html")

    for (html <- htmls) {
      val xml = XML.loadString(Source.fromFile(new File(outputDir, html)).getLines.mkString)
      val links = for (node <- xml \\ "a") yield {
        node.attribute("href") match {
          case Some(url) => url.toString
          case None => fail()
        }
      }
   
      assert( links.toSet == Set("ClassInMainDir.scala.html", "subdir/ClassInSubDir.scala.html") )
    }
  }

  test("HTML coverage report escapes HTML") {

    val coverage = Coverage()
    coverage.add(statementForClassContainingHtml)
    val outputDir = writeCoverageToTemporaryDir(coverage)

    val contentsOfFileWithEmbeddedHtml = Source.fromFile(new File(outputDir, "ClassContainingHtml.scala.html")).getLines.mkString
    assert( !contentsOfFileWithEmbeddedHtml.contains("<div>HTML content</div>") )
    assert( contentsOfFileWithEmbeddedHtml.contains("&lt;div&gt;HTML content&lt;/div&gt;") )
  }
}
