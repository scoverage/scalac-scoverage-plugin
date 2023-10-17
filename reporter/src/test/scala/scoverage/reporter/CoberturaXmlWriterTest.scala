package scoverage.reporter

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.SAXParserFactory

import scala.xml.Elem
import scala.xml.XML
import scala.xml.factory.XMLLoader

import munit.FunSuite
import org.xml.sax.ErrorHandler
import org.xml.sax.SAXParseException
import scoverage.domain.Coverage

import TestUtils._
import BaseReportWriter.failIfNoSourceRoot

/** @author Stephen Samuel */
class CoberturaXmlWriterTest extends FunSuite {

  val xmlOutputPath = FunFixture[Path](
    setup = test => {
      val dir = Files.createTempDirectory("test-cobertura")
      dir.resolve("cobertura.xml")
    },
    teardown = file => {
      Files.deleteIfExists(file)
      Files.deleteIfExists(file.getParent())
    }
  )

  // Let the current directory be our source root (any dir would do)
  val sourceRoot = new File(".")

  def canonicalPath(fileName: String) =
    new File(sourceRoot, fileName).getCanonicalPath

  def relativePath(fileName: String) =
    new File(sourceRoot, fileName).getPath.replace("./", "")

  def parseXML(file: Path): Elem = {
    // Needed to acount for https://github.com/scala/scala-xml/pull/177
    val customXML: XMLLoader[Elem] = XML.withSAXParser {
      val factory = SAXParserFactory.newInstance()
      factory.setFeature(
        "http://apache.org/xml/features/nonvalidating/load-external-dtd",
        false
      )
      factory.newSAXParser()
    }
    customXML.loadFile(file.toFile())
  }

  xmlOutputPath.test("cobertura output has relative file path") { xmlPath =>
    val coverage = Coverage()
    val outputDir = xmlPath.getParent().toFile()
    coverage.add(
      testStatement(
        testLocation(canonicalPath("a.scala"))
      )
    )
    coverage.add(
      testStatement(
        testLocation(canonicalPath("a/b.scala"))
      )
    )
    val writer =
      new CoberturaXmlWriter(sourceRoot, outputDir, None, failIfNoSourceRoot)
    writer.write(coverage)

    val xml = parseXML(xmlPath)

    assertEquals(
      ((xml \\ "coverage" \ "packages" \ "package" \ "classes" \ "class")(
        0
      ) \ "@filename").text,
      relativePath("a.scala")
    )

    assertEquals(
      ((xml \\ "coverage" \ "packages" \ "package" \ "classes" \ "class")(
        1
      ) \ "@filename").text,
      relativePath("a/b.scala")
    )
  }

  xmlOutputPath.test("cobertura output validates") { xmlPath =>
    val coverage = Coverage()
    val outputDir = xmlPath.getParent().toFile()

    val fakeSources = Seq("a.scala", "b.scala", "c.scala", "d.scala")
    for {
      s <- fakeSources
      loc = testLocation(canonicalPath(s))
      isBranch <- Seq(true, false)
      invokeCount <- Seq(0, 3)
    } coverage.add(testStatement(loc, isBranch, invokeCount))

    val writer =
      new CoberturaXmlWriter(sourceRoot, outputDir, None, failIfNoSourceRoot)
    writer.write(coverage)

    val domFactory = DocumentBuilderFactory.newInstance()
    domFactory.setValidating(true)
    val builder = domFactory.newDocumentBuilder()
    builder.setErrorHandler(new ErrorHandler() {
      override def error(e: SAXParseException): Unit = {
        fail(e.getMessage(), e.getCause())
      }
      override def fatalError(e: SAXParseException): Unit = {
        fail(e.getMessage(), e.getCause())
      }
      override def warning(e: SAXParseException): Unit = {
        fail(e.getMessage(), e.getCause())
      }
    })
    builder.parse(xmlPath.toFile())
  }

  xmlOutputPath.test(
    "coverage rates are written as 2dp decimal values rather than percentage"
  ) { xmlPath =>
    val coverage = Coverage()
    val outputDir = xmlPath.getParent().toFile()
    val fakeSourcePath = canonicalPath("a.scala")
    coverage.add(
      testStatement(
        testLocation(fakeSourcePath),
        isBranch = false,
        invokeCount = 0 // not covered
      )
    )
    coverage.add(
      testStatement(
        testLocation(fakeSourcePath),
        isBranch = true,
        invokeCount = 0 // not covered
      )
    )
    coverage.add(
      testStatement(
        testLocation(fakeSourcePath),
        isBranch = true,
        invokeCount = 1 // covered
      )
    )

    val writer =
      new CoberturaXmlWriter(sourceRoot, outputDir, None, failIfNoSourceRoot)
    writer.write(coverage)

    val xml = parseXML(xmlPath)

    assertEquals((xml \\ "coverage" \ "@line-rate").text, "0.33", "line-rate")
    assertEquals(
      (xml \\ "coverage" \ "@branch-rate").text,
      "0.50",
      "branch-rate"
    )

  }

  def testPathRecovery(name: String, policy: BaseReportWriter.PathRecoverer)(checks: Elem => Unit)(implicit loc: munit.Location) = {
    xmlOutputPath.test(name) { xmlPath =>
      val outputDir = xmlPath.getParent().toFile()
      val coverage = Coverage()

      val notInRoot = "/*not*/in/root.scala"
      val inRoot = "in-root.sc"

      coverage.add(
        testStatement(
          testLocation(notInRoot, className = "A") // should be replaced
        )
      )
      coverage.add(
        testStatement(
          testLocation(
            canonicalPath(inRoot),
            className = "B"
          ) // should be unchanged
        )
      )
      val writer = new CoberturaXmlWriter(
        sourceRoot,
        outputDir,
        None,
        policy
      )
      writer.write(coverage)

      val xml = parseXML(xmlPath)
      checks(xml)
    }
  }

  testPathRecovery("path recovery replace", (f, roots) => Some("recovered/path")) { xml =>
    val classes =
      (xml \\ "coverage" \ "packages" \ "package" \ "classes" \ "class")

    assertEquals(
      (classes(0) \ "@filename").text,
      "recovered/path"
    )
    assertEquals(
      (classes(1) \ "@filename").text,
      relativePath("in-root.sc")
    )
  }

  testPathRecovery("path recovery: skip", (f, roots) => None) { xml =>
    val classes =
      (xml \\ "coverage" \ "packages" \ "package" \ "classes" \ "class")

    println(classes)
    assertEquals(
      (classes(0) \ "@filename").text,
      "in-root.sc"
    )
    assertEquals(
      classes.length,
      1
    )
  }
}
