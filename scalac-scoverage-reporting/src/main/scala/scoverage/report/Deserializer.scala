package scoverage
package report

import scoverage._
import java.io._
import scala.io.Source
import scala.xml.{Utility, XML}

object Deserializer {

  def deserialize(str: String): Coverage = {
    val xml = XML.loadString(str)
    val statements = xml \ "statement" map (node => {
      val source = (node \ "source").text
      val count = (node \ "count").text.toInt
      val ignored = (node \ "ignored").text.toBoolean
      val branch = (node \ "branch").text.toBoolean
      val _package = (node \ "package").text
      val _class = (node \ "class").text
      val fullClassName = (node \ "fullClassName").text
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
        Location(_package, _class, fullClassName, classType, method, path),
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
}
