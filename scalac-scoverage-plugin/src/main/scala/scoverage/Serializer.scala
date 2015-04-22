package scoverage

import java.io._

import scala.io.Source
import scala.xml.{Utility, XML}

object Serializer {

  // Write out coverage data to the given data directory, using the default coverage filename
  def serialize(coverage: Coverage, dataDir: String): Unit = serialize(coverage, coverageFile(dataDir))

  // Write out coverage data to given file.
  def serialize(coverage: Coverage, file: File): Unit = {
    val writer = new BufferedWriter(new FileWriter(file))
    serialize(coverage, writer)
    writer.close()
  }

  def serialize(coverage: Coverage, writer: Writer): Unit = {
    def writeStatement(stmt: Statement, writer: Writer): Unit = {
      writer.write {
        val xml = <statement>
          <source>
            {stmt.source}
          </source>
          <package>
            {stmt.location.packageName}
          </package>
          <class>
            {stmt.location.className}
          </class>
          <classType>
            {stmt.location.classType.toString}
          </classType>
          <topLevelClass>
            {stmt.location.topLevelClass}
          </topLevelClass>
          <method>
            {stmt.location.method}
          </method>
          <path>
            {stmt.location.sourcePath}
          </path>
          <id>
            {stmt.id.toString}
          </id>
          <start>
            {stmt.start.toString}
          </start>
          <end>
            {stmt.end.toString}
          </end>
          <line>
            {stmt.line.toString}
          </line>
          <description>
            {escape(stmt.desc)}
          </description>
          <symbolName>
            {escape(stmt.symbolName)}
          </symbolName>
          <treeName>
            {escape(stmt.treeName)}
          </treeName>
          <branch>
            {stmt.branch.toString}
          </branch>
          <count>
            {stmt.count.toString}
          </count>
          <ignored>
            {stmt.ignored.toString}
          </ignored>
        </statement>
        Utility.trim(xml) + "\n"
      }
    }
    writer.write("<statements>\n")
    coverage.statements.foreach(stmt => writeStatement(stmt, writer))
    writer.write("</statements>")
  }

  def coverageFile(dataDir: File): File = coverageFile(dataDir.getAbsolutePath)
  def coverageFile(dataDir: String): File = new File(dataDir, Constants.CoverageFileName)

  def deserialize(str: String): Coverage = {
    val xml = XML.loadString(str)
    val statements = xml \ "statement" map (node => {
      val source = (node \ "source").text
      val count = (node \ "count").text.toInt
      val ignored = (node \ "ignored").text.toBoolean
      val branch = (node \ "branch").text.toBoolean
      val _package = (node \ "package").text
      val _class = (node \ "class").text
      val topLevelClass = (node \ "topLevelClass").text
      val method = (node \ "method").text
      val path = (node \ "path").text
      val treeName = (node \ "treeName").text
      val symbolName = (node \ "symbolName").text
      val id = (node \ "id").text.toInt
      val line = (node \ "line").text.toInt
      val desc = (node \ "description").text
      val start = (node \ "start").text.toInt
      val end = (node \ "end").text.toInt
      val classType = (node \ "classType").text match {
        case "Trait" => ClassType.Trait
        case "Object" => ClassType.Object
        case _ => ClassType.Class
      }
      Statement(source,
        Location(_package, _class, topLevelClass, classType, method, path),
        id,
        start,
        end,
        line,
        desc,
        symbolName,
        treeName, branch, count, ignored)
    })

    val coverage = Coverage()
    for ( statement <- statements )
      if (statement.ignored) coverage.addIgnoredStatement(statement)
      else coverage.add(statement)
    coverage
  }

  def deserialize(file: File): Coverage = {
    val str = Source.fromFile(file).mkString
    deserialize(str)
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
