package scoverage

import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.{ErrorHandler, SAXParseException}
import java.io.File
import scoverage.report.CoberturaXmlWriter
import org.scalatest.{FunSuite, BeforeAndAfter, OneInstancePerTest}
import org.apache.commons.io.FileUtils

/** @author Stephen Samuel */
class CoberturaXmlWriterTest extends FunSuite with BeforeAndAfter with OneInstancePerTest {

  test("cobertura output validates") {

    val file = new File(FileUtils.getTempDirectoryPath + "/cobertura.xml")
    file.deleteOnExit()

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

    val writer = new CoberturaXmlWriter(new File(""), FileUtils.getTempDirectory)
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
    builder.parse(file)
  }
}
