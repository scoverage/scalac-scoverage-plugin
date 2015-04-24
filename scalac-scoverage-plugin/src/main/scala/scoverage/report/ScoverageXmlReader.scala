package scoverage.report

import java.io.File

import scoverage._

import scala.xml.XML

/**
 * Reads in an Scoverage XML report and converts to a Coverage instance.
 */
object ScoverageXmlReader {

  def read(file: File): Coverage = {
    val xml = XML.loadFile(file)

    var id = 0
    val coverage = Coverage()
    (xml \\ "statement") foreach { node =>

      val source = node \ "@source"
      val pkg = node \ "@package"
      val classname = node \ "@class"
      val classType = node \ "@class-type"
      val topLevelClass = node \ "@top-level-class"
      val method = node \ "@method"
      val start = node \ "@start"
      val end = node \ "@end"
      val line = node \ "@line"
      val branch = node \ "@branch"
      val count = node \ "@invocation-count"
      val symbolName = node \ "@symbol"
      val treeName = node \ "@tree"
      val ignored = node \ "@ignored"

      val location = Location(pkg.text,
        classname.text,
        topLevelClass.text,
        ClassType fromString classType.text,
        method.text,
        source.text)

      id = id + 1

      coverage add Statement(
        source.text,
        location,
        id,
        start.text.toInt,
        end.text.toInt,
        line.text.toInt,
        "", // not interested in debug info when re-creating
        symbolName.text,
        treeName.text,
        branch.text.toBoolean,
        count.text.toInt,
        ignored.text.toBoolean
      )
    }
    coverage
  }
}
