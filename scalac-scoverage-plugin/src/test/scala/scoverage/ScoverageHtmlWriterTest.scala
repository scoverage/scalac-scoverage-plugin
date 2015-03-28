package scoverage

import java.io._
import java.util.UUID
import scala.io.Source

import scala.xml.XML

import scoverage.report.ScoverageHtmlWriter

import org.scalatest.FunSuite

class ScoverageHtmlWriterTest extends FunSuite {

  test("HTML coverage report has been created correctly") {

    def tempDir(): File = {
      val dir = new File(IOUtils.getTempDirectory, UUID.randomUUID().toString)
      dir.mkdirs()
      dir.deleteOnExit()
      dir
    }

    val coverage = Coverage()

    val class2 = new File(getClass.getResource("forHtmlWriter/src/main/scala/subdir/Class2.scala").getFile()).getCanonicalPath()
    val class1 = new File(getClass.getResource("forHtmlWriter/src/main/scala/Class1.scala").getFile()).getCanonicalPath()

    coverage.add(
      Statement(class2,
        Location("coverage.sample", "Class2", "Class2", ClassType.Class, "msg_test", class2),
        2, 60, 80, 4, "scala.this.Predef.println(\"test code\")",
        "scala.Predef.println", "Apply", false, 0)
    )

    coverage.add(
      Statement(class1,
        Location("coverage.sample", "Class1", "Class1", ClassType.Class, "msg_coverage", class1),
        1, 64, 99, 4, "scala.this.Predef.println(\"measure coverage of code\")",
        "scala.Predef.println", "Apply", false, 0)
    )

    val dir = getClass.getResource("forHtmlWriter/src/main/scala/").getFile()
    val outputDir = tempDir()

    val htmlWriter = new ScoverageHtmlWriter(new File(dir), outputDir)
    htmlWriter.write(coverage)

    val htmls = List("overview.html", "coverage.sample.html")

    for (html <- htmls) {
      val xml = XML.loadString(Source.fromFile(new File(outputDir, html)).getLines.mkString)
      val links = for (node <- xml \\ "a") yield {
        node.attribute("href") match {
          case Some(url) => url.toString
          case None => fail()
        }
      }
   
      assert( links.toSet == Set("Class1.scala.html", "subdir/Class2.scala.html") )
    }
  }
}
