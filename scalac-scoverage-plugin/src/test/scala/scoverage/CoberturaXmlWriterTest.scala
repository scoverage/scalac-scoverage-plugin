package scoverage

import java.io.File
import java.util.UUID
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.SAXParserFactory

import scala.xml.Elem
import scala.xml.XML
import scala.xml.factory.XMLLoader

import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.funsuite.AnyFunSuite
import org.xml.sax.ErrorHandler
import org.xml.sax.SAXParseException
import scoverage.report.CoberturaXmlWriter

/** @author Stephen Samuel */
class CoberturaXmlWriterTest
    extends AnyFunSuite
    with BeforeAndAfter
    with OneInstancePerTest {

  def tempDir(): File = {
    val dir = new File(IOUtils.getTempDirectory, UUID.randomUUID.toString)
    dir.mkdirs()
    dir.deleteOnExit()
    dir
  }

  def fileIn(dir: File) = new File(dir, "cobertura.xml")

  // Let current directory be our source root
  private val sourceRoot = new File(".")
  private def canonicalPath(fileName: String) =
    new File(sourceRoot, fileName).getCanonicalPath

  test("cobertura output validates") {

    val dir = tempDir()

    val coverage = scoverage.Coverage()
    coverage
      .add(
        Statement(
          Location(
            "com.sksamuel.scoverage",
            "A",
            "com.sksamuel.scoverage.A",
            ClassType.Object,
            "create",
            canonicalPath("a.scala")
          ),
          1,
          2,
          3,
          12,
          "",
          "",
          "",
          false,
          3
        )
      )
    coverage
      .add(
        Statement(
          Location(
            "com.sksamuel.scoverage",
            "A",
            "com.sksamuel.scoverage.A",
            ClassType.Object,
            "create2",
            canonicalPath("a.scala")
          ),
          2,
          2,
          3,
          16,
          "",
          "",
          "",
          false,
          3
        )
      )
    coverage
      .add(
        Statement(
          Location(
            "com.sksamuel.scoverage2",
            "B",
            "com.sksamuel.scoverage2.B",
            ClassType.Object,
            "retrieve",
            canonicalPath("b.scala")
          ),
          3,
          2,
          3,
          21,
          "",
          "",
          "",
          false,
          0
        )
      )
    coverage
      .add(
        Statement(
          Location(
            "com.sksamuel.scoverage2",
            "B",
            "B",
            ClassType.Object,
            "retrieve2",
            canonicalPath("b.scala")
          ),
          4,
          2,
          3,
          9,
          "",
          "",
          "",
          false,
          3
        )
      )
    coverage
      .add(
        Statement(
          Location(
            "com.sksamuel.scoverage3",
            "C",
            "com.sksamuel.scoverage3.C",
            ClassType.Object,
            "update",
            canonicalPath("c.scala")
          ),
          5,
          2,
          3,
          66,
          "",
          "",
          "",
          true,
          3
        )
      )
    coverage
      .add(
        Statement(
          Location(
            "com.sksamuel.scoverage3",
            "C",
            "com.sksamuel.scoverage3.C",
            ClassType.Object,
            "update2",
            canonicalPath("c.scala")
          ),
          6,
          2,
          3,
          6,
          "",
          "",
          "",
          true,
          3
        )
      )
    coverage
      .add(
        Statement(
          Location(
            "com.sksamuel.scoverage4",
            "D",
            "com.sksamuel.scoverage4.D",
            ClassType.Object,
            "delete",
            canonicalPath("d.scala")
          ),
          7,
          2,
          3,
          4,
          "",
          "",
          "",
          false,
          0
        )
      )
    coverage
      .add(
        Statement(
          Location(
            "com.sksamuel.scoverage4",
            "D",
            "com.sksamuel.scoverage4.D",
            ClassType.Object,
            "delete2",
            canonicalPath("d.scala")
          ),
          8,
          2,
          3,
          14,
          "",
          "",
          "",
          false,
          0
        )
      )

    val writer = new CoberturaXmlWriter(sourceRoot, dir)
    writer.write(coverage)

    val domFactory = DocumentBuilderFactory.newInstance()
    domFactory.setValidating(true)
    val builder = domFactory.newDocumentBuilder()
    builder.setErrorHandler(new ErrorHandler() {
      @Override
      def error(e: SAXParseException): Unit = {
        fail(e)
      }
      @Override
      def fatalError(e: SAXParseException): Unit = {
        fail(e)
      }

      @Override
      def warning(e: SAXParseException): Unit = {
        fail(e)
      }
    })
    builder.parse(fileIn(dir))
  }

  test(
    "coverage rates are written as 2dp decimal values rather than percentage"
  ) {

    val dir = tempDir()

    val coverage = Coverage()
    coverage
      .add(
        Statement(
          Location(
            "com.sksamuel.scoverage",
            "A",
            "com.sksamuel.scoverage.A",
            ClassType.Object,
            "create",
            canonicalPath("a.scala")
          ),
          1,
          2,
          3,
          12,
          "",
          "",
          "",
          false
        )
      )
    coverage
      .add(
        Statement(
          Location(
            "com.sksamuel.scoverage",
            "A",
            "com.sksamuel.scoverage.A",
            ClassType.Object,
            "create2",
            canonicalPath("a.scala")
          ),
          2,
          2,
          3,
          16,
          "",
          "",
          "",
          true
        )
      )
    coverage
      .add(
        Statement(
          Location(
            "com.sksamuel.scoverage",
            "A",
            "com.sksamuel.scoverage.A",
            ClassType.Object,
            "create3",
            canonicalPath("a.scala")
          ),
          3,
          3,
          3,
          20,
          "",
          "",
          "",
          true,
          1
        )
      )

    val writer = new CoberturaXmlWriter(sourceRoot, dir)
    writer.write(coverage)

    val customXML: XMLLoader[Elem] = XML.withSAXParser {
      val factory = SAXParserFactory.newInstance()
      factory.setFeature(
        "http://apache.org/xml/features/nonvalidating/load-external-dtd",
        false
      )
      factory.newSAXParser()
    }

    val xml = customXML.loadFile(fileIn(dir))

    assert((xml \\ "coverage" \ "@line-rate").text === "0.33", "line-rate")
    assert((xml \\ "coverage" \ "@branch-rate").text === "0.50", "branch-rate")

  }
}
