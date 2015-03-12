package scoverage

import java.io.File
import java.util.{Locale, UUID}
import javax.xml.parsers.DocumentBuilderFactory

import org.scalatest.{BeforeAndAfter, FunSuite, OneInstancePerTest}
import org.xml.sax.{ErrorHandler, SAXParseException}
import scoverage.report.CoberturaXmlWriter

import scala.xml.XML

/** @author Stephen Samuel */
class CoberturaXmlWriterTest extends FunSuite with BeforeAndAfter with OneInstancePerTest {

  def tempDir(): File = {
    val dir = new File(IOUtils.getTempDirectory, UUID.randomUUID().toString)
    dir.mkdirs()
    dir.deleteOnExit()
    dir
  }

  def fileIn(dir: File) = new File(dir, "cobertura.xml")

  // Let current directory be our source root
  private val sourceRoot = new File(".")
  private def canonicalPath(fileName: String) = new File(sourceRoot, fileName).getCanonicalPath

  test("cobertura output validates") {

    val dir = tempDir()

    val coverage = scoverage.Coverage()
    coverage
      .add(Statement(canonicalPath("a.scala"), Location("com.sksamuel.scoverage", "A", "A", ClassType.Object, "create", canonicalPath("a.scala")),
      1, 2, 3, 12, "", "", "", false, 3))
    coverage
      .add(Statement(canonicalPath("a.scala"), Location("com.sksamuel.scoverage", "A", "A", ClassType.Object, "create2", canonicalPath("a.scala")),
      2, 2, 3, 16, "", "", "", false, 3))
    coverage
      .add(Statement(canonicalPath("b.scala"), Location("com.sksamuel.scoverage2", "B", "B", ClassType.Object, "retrieve", canonicalPath("b.scala")),
      3, 2, 3, 21, "", "", "", false, 0))
    coverage
      .add(Statement(canonicalPath("b.scala"),
      Location("com.sksamuel.scoverage2", "B", "B", ClassType.Object, "retrieve2", canonicalPath("b.scala")),
      4, 2, 3, 9, "", "", "", false, 3))
    coverage
      .add(Statement(canonicalPath("c.scala"), Location("com.sksamuel.scoverage3", "C", "C", ClassType.Object, "update", canonicalPath("c.scala")),
      5, 2, 3, 66, "", "", "", true, 3))
    coverage
      .add(Statement(canonicalPath("c.scala"), Location("com.sksamuel.scoverage3", "C", "C", ClassType.Object, "update2", canonicalPath("c.scala")),
      6, 2, 3, 6, "", "", "", true, 3))
    coverage
      .add(Statement(canonicalPath("d.scala"), Location("com.sksamuel.scoverage4", "D", "D", ClassType.Object, "delete", canonicalPath("d.scala")),
      7, 2, 3, 4, "", "", "", false, 0))
    coverage
      .add(Statement(canonicalPath("d.scala"), Location("com.sksamuel.scoverage4", "D", "D", ClassType.Object, "delete2", canonicalPath("d.scala")),
      8, 2, 3, 14, "", "", "", false, 0))

    val writer = new CoberturaXmlWriter(sourceRoot, dir)
    writer.write(coverage)

    val domFactory = DocumentBuilderFactory.newInstance()
    domFactory.setValidating(true)
    val builder = domFactory.newDocumentBuilder()
    builder.setErrorHandler(new ErrorHandler() {
      @Override
      def error(e: SAXParseException) {
        fail(e)
      }
      @Override
      def fatalError(e: SAXParseException) {
        fail(e)
      }

      @Override
      def warning(e: SAXParseException) {
        fail(e)
      }
    })
    builder.parse(fileIn(dir))
  }

  test("coverage rates are written as 2dp decimal values rather than percentage") {

    val dir = tempDir()

    val coverage = Coverage()
    coverage
      .add(Statement(canonicalPath("a.scala"), Location("com.sksamuel.scoverage", "A", "A", ClassType.Object, "create", canonicalPath("a.scala")),
      1, 2, 3, 12, "", "", "", false))
    coverage
      .add(Statement(canonicalPath("a.scala"), Location("com.sksamuel.scoverage", "A", "A", ClassType.Object, "create2", canonicalPath("a.scala")),
      2, 2, 3, 16, "", "", "", true))
    coverage
      .add(Statement(canonicalPath("a.scala"), Location("com.sksamuel.scoverage", "A", "A", ClassType.Object, "create3", canonicalPath("a.scala")),
      3, 3, 3, 20, "", "", "", true, 1))

    val writer = new CoberturaXmlWriter(sourceRoot, dir)
    writer.write(coverage)

    val xml = XML.loadFile(fileIn(dir))

    def formattedLocally(decimal: BigDecimal) = "%.2f".format(decimal)

    assert(xml \\ "coverage" \@ "line-rate" === formattedLocally(0.33), "line-rate")
    assert(xml \\ "coverage" \@ "branch-rate" === formattedLocally(0.50), "branch-rate")

  }
}
