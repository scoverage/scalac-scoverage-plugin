package scoverage.report

import java.io.File
import java.util.UUID
import javax.xml.parsers.DocumentBuilderFactory

import org.apache.commons.io.FileUtils
import org.scalatest.{BeforeAndAfter, FunSuite, OneInstancePerTest}
import org.xml.sax.{ErrorHandler, SAXParseException}
import scoverage.{ClassType, Coverage, Location, MeasuredStatement}

import scala.xml.XML

/** @author Stephen Samuel */
class CoberturaXmlWriterTest extends FunSuite with BeforeAndAfter with OneInstancePerTest {

  def tempDir(): File = {
    val dir = new File(FileUtils.getTempDirectory, UUID.randomUUID().toString)
    dir.mkdirs()
    dir.deleteOnExit()
    dir
  }
  
  def fileIn(dir: File) = new File(dir, "cobertura.xml")
  
  test("cobertura output validates") {

    val dir = tempDir()

    val coverage = Coverage()
    coverage.add(MeasuredStatement("a.scala", Location("com.sksamuel.scoverage", "A", ClassType.Object, "create"),
      1, 2, 3, 12, "", "", "", false, 3))
    coverage.add(MeasuredStatement("a.scala", Location("com.sksamuel.scoverage", "A", ClassType.Object, "create2"),
      2, 2, 3, 16, "", "", "", false, 3))
    coverage.add(MeasuredStatement("b.scala", Location("com.sksamuel.scoverage2", "B", ClassType.Object, "retrieve"),
      3, 2, 3, 21, "", "", "", false, 0))
    coverage.add(MeasuredStatement("b.scala", Location("com.sksamuel.scoverage2", "B", ClassType.Object, "retrieve2"),
      4, 2, 3, 9, "", "", "", false, 3))
    coverage.add(MeasuredStatement("c.scala", Location("com.sksamuel.scoverage3", "C", ClassType.Object, "update"),
      5, 2, 3, 66, "", "", "", true, 3))
    coverage.add(MeasuredStatement("c.scala", Location("com.sksamuel.scoverage3", "C", ClassType.Object, "update2"),
      6, 2, 3, 6, "", "", "", true, 3))
    coverage.add(MeasuredStatement("d.scala", Location("com.sksamuel.scoverage4", "D", ClassType.Object, "delete"),
      7, 2, 3, 4, "", "", "", false, 0))
    coverage.add(MeasuredStatement("d.scala", Location("com.sksamuel.scoverage4", "D", ClassType.Object, "delete2"),
      8, 2, 3, 14, "", "", "", false, 0))

    val writer = new CoberturaXmlWriter(new File(""), dir)
    writer.write(coverage)

    val domFactory = DocumentBuilderFactory.newInstance()
    domFactory.setValidating(true)
    val builder = domFactory.newDocumentBuilder()
    builder.setErrorHandler(new ErrorHandler() {
      @Override
      def error(e: SAXParseException) {
        e.printStackTrace()
        assert(false)
      }
      @Override
      def fatalError(e: SAXParseException) {
        e.printStackTrace()
        assert(false)
      }

      @Override
      def warning(e: SAXParseException) {
        e.printStackTrace()
        assert(false)
      }
    })
    builder.parse(fileIn(dir))
  }

  test("coverage rates are written as 2dp decimal values rather than percentage") {

    val dir = tempDir()

    val coverage = Coverage()
    coverage.add(MeasuredStatement("a.scala", Location("com.sksamuel.scoverage", "A", ClassType.Object, "create"),
      1, 2, 3, 12, "", "", "", false))
    coverage.add(MeasuredStatement("a.scala", Location("com.sksamuel.scoverage", "A", ClassType.Object, "create2"),
      2, 2, 3, 16, "", "", "", true))
    coverage.add(MeasuredStatement("a.scala", Location("com.sksamuel.scoverage", "A", ClassType.Object, "create3"),
      3, 3, 3, 20, "", "", "", true, 1))

    val writer = new CoberturaXmlWriter(new File(""), dir)
    writer.write(coverage)

    val xml = XML.loadFile(fileIn(dir))

    assert(xml \\ "coverage" \@ "line-rate" === "0.33", "line-rate")
    assert(xml \\ "coverage" \@ "branch-rate" === "0.50", "branch-rate")

  }
}
