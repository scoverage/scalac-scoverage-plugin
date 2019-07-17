package scoverage.report

import java.io.File

import scoverage._

import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamWriter
import java.io.FileWriter
import com.sun.xml.internal.txw2.output.IndentingXMLStreamWriter

/** @author Stephen Samuel */
class ScoverageXmlWriter(sourceDirectories: Seq[File], outputDir: File, debug: Boolean) extends BaseReportWriter(sourceDirectories, outputDir) {

  def this (sourceDir: File, outputDir: File, debug: Boolean) {
    this(Seq(sourceDir), outputDir, debug)
  }

  def write(coverage: Coverage): Unit = {
    val file = IOUtils.reportFile(outputDir, debug)

    val factory = XMLOutputFactory.newInstance()
    val w: XMLStreamWriter = factory.createXMLStreamWriter(new FileWriter(file))
    val writer = new IndentingXMLStreamWriter(w)
    xml(coverage, writer)
  }

  private def xml(coverage: Coverage, writer: XMLStreamWriter): Unit = {
    writer.writeStartDocument()
    writer.writeStartElement("scoverage")
    writer.writeAttribute("statement-count", coverage.statementCount.toString)
    writer.writeAttribute("statements-invoked", coverage.invokedStatementCount.toString)
    writer.writeAttribute("statement-rate", coverage.statementCoverageFormatted)
    writer.writeAttribute("branch-rate", coverage.branchCoverageFormatted)
    writer.writeAttribute("version", "1.0")
    writer.writeAttribute("timestamp", System.currentTimeMillis.toString)

    writer.writeStartElement("packages")
        coverage.packages.foreach{m: MeasuredPackage => pack(m,writer)}
    writer.writeEndElement()
    writer.writeEndElement()
    writer.writeEndDocument()

    writer.flush()
    writer.close()
  }

  private def statement(stmt: Statement, writer: XMLStreamWriter): Unit = {
    debug match {
      case true =>
        writer.writeStartElement("statement")
        writer.writeAttribute("package", stmt.location.packageName)
        writer.writeAttribute("class", stmt.location.className)
        writer.writeAttribute("class-type", stmt.location.classType.toString)
        writer.writeAttribute("full-class-name", stmt.location.fullClassName)
        writer.writeAttribute("source", stmt.source)
        writer.writeAttribute("method", stmt.location.method)
        writer.writeAttribute("start", stmt.start.toString)
        writer.writeAttribute("end", stmt.end.toString)
        writer.writeAttribute("line", stmt.line.toString)
        writer.writeAttribute("symbol", escape(stmt.symbolName))
        writer.writeAttribute("tree", escape(stmt.treeName))
        writer.writeAttribute("branch", stmt.branch.toString)
        writer.writeAttribute("invocation-count", stmt.count.toString)
        writer.writeAttribute("ignored", stmt.ignored.toString)
        writer.writeCharacters(escape(stmt.desc))
        writer.writeEndElement()
      case false =>
        writer.writeStartElement("statement")
        writer.writeAttribute("package", stmt.location.packageName)
        writer.writeAttribute("class", stmt.location.className)
        writer.writeAttribute("class-type", stmt.location.classType.toString)
        writer.writeAttribute("full-class-name", stmt.location.fullClassName)
        writer.writeAttribute("source", stmt.source)
        writer.writeAttribute("method", stmt.location.method)
        writer.writeAttribute("start", stmt.start.toString)
        writer.writeAttribute("end", stmt.end.toString)
        writer.writeAttribute("line", stmt.line.toString)
        writer.writeAttribute("branch", stmt.branch.toString)
        writer.writeAttribute("invocation-count", stmt.count.toString)
        writer.writeAttribute("ignored", stmt.ignored.toString)
        writer.writeEndElement()
    }
  }

  private def method(method: MeasuredMethod, writer: XMLStreamWriter): Unit = {
    writer.writeStartElement("method")
    writer.writeAttribute("name", method.name)
    writer.writeAttribute("statement-count", method.statementCount.toString)
    writer.writeAttribute("statements-invoked", method.invokedStatementCount.toString)
    writer.writeAttribute("statement-rate",  method.statementCoverageFormatted)
    writer.writeAttribute("branch-rate",method.branchCoverageFormatted)

    writer.writeStartElement("statements")
      method.statements.foreach{s: Statement => statement(s, writer)}
    writer.writeEndElement()
    writer.writeEndElement()

  }

  private def klass(klass: MeasuredClass, writer: XMLStreamWriter): Unit = {
    writer.writeStartElement("class")
    writer.writeAttribute("name", klass.fullClassName)
    writer.writeAttribute("filename", relativeSource(klass.source))
    writer.writeAttribute("statement-count", klass.statementCount.toString)
    writer.writeAttribute("statements-invoked", klass.invokedStatementCount.toString)
    writer.writeAttribute("statement-rate",  klass.statementCoverageFormatted)
    writer.writeAttribute("branch-rate",klass.branchCoverageFormatted)

    writer.writeStartElement("methods")
      klass.methods.foreach{m: MeasuredMethod => method(m, writer)}
    writer.writeEndElement()
    writer.writeEndElement()
  }

  private def pack(pack: MeasuredPackage, writer: XMLStreamWriter): Unit = {
    writer.writeStartElement("package")
    writer.writeAttribute("name", pack.name)
    writer.writeAttribute("statement-count", pack.statementCount.toString)
    writer.writeAttribute("statements-invoked", pack.invokedStatementCount.toString)
    writer.writeAttribute("statement-rate", pack.statementCoverageFormatted)

    writer.writeStartElement("classes")
      pack.classes.foreach{k: MeasuredClass => klass(k, writer)}
    writer.writeEndElement()
    writer.writeEndElement()
  }
  /**
   * This method ensures that the output String has only
   * valid XML unicode characters as specified by the
   * XML 1.0 standard. For reference, please see
   * <a href="http://www.w3.org/TR/2000/REC-xml-20001006#NT-Char">the
   * standard</a>. This method will return an empty
   * String if the input is null or empty.
   *
   * @param in The String whose non-valid characters we want to remove.
   * @return The in String, stripped of non-valid characters.
   * @see http://blog.mark-mclaren.info/2007/02/invalid-xml-characters-when-valid-utf8_5873.html
   *
   */
  def escape(in: String): String = {
    val out = new StringBuilder()
    for ( current <- Option(in).getOrElse("").toCharArray ) {
      if ((current == 0x9) || (current == 0xA) || (current == 0xD) ||
        ((current >= 0x20) && (current <= 0xD7FF)) ||
        ((current >= 0xE000) && (current <= 0xFFFD)) ||
        ((current >= 0x10000) && (current <= 0x10FFFF)))
        out.append(current)
    }
    out.mkString
  }

}
