package scoverage.domain

import scala.io.Codec
import scala.io.Source

/** @author Stephen Samuel */
class CodeGrid(mFile: MeasuredFile, sourceEncoding: Option[String]) {

  // for backward compatibility only
  def this(mFile: MeasuredFile) = {
    this(mFile, None);
  }

  case class Cell(char: Char, var status: StatementStatus)

  private val lineBreak = "\n"

  // Array of lines, each line is an array of cells, where a cell is a character + coverage info for that position
  // All cells default to NoData until the highlighted information is applied
  // note: we must re-include the line sep to keep source positions correct.
  private val lines = source(mFile)
    .split(lineBreak)
    .map(line => (line.toCharArray ++ lineBreak).map(Cell(_, NoData)))

  // useful to have a single array to write into the cells
  private val cells = lines.flatten

  // apply the instrumentation data to the cells updating their coverage info
  mFile.statements.foreach(stmt => {
    for (k <- stmt.start until stmt.end) {
      if (k < cells.size) {
        // if the cell is set to Invoked, then it be changed to NotInvoked, as an inner statement will override
        // outer containing statements. If a cell is NotInvoked then it can not be changed further.
        // in that block were executed
        cells(k).status match {
          case Invoked => if (!stmt.isInvoked) cells(k).status = NotInvoked
          case NoData =>
            if (!stmt.isInvoked) cells(k).status = NotInvoked
            else if (stmt.isInvoked) cells(k).status = Invoked
          case NotInvoked =>
        }
      }
    }
  })

  val highlighted: String = {
    var lineNumber = 1
    val code = lines map (line => {
      var style = cellStyle(NoData)
      val sb = new StringBuilder
      sb append lineNumber append " "
      lineNumber = lineNumber + 1
      sb append spanStart(NoData)
      line.map(cell => {
        val style2 = cellStyle(cell.status)
        if (style != style2) {
          sb append "</span>"
          sb append spanStart(cell.status)
          style = style2
        }
        // escape xml characters
        cell.char match {
          case '<' => sb.append("&lt;")
          case '>' => sb.append("&gt;")
          case '&' => sb.append("&amp;")
          case '"' => sb.append("&quot;")
          case c   => sb.append(c)
        }
      })
      sb append "</span>"
      sb.toString
    }) mkString ""
    s"<pre style='font-size: 12pt; font-family: courier, monospace;'>$code</pre>"
  }

  private def source(mfile: MeasuredFile): String = {
    val src = sourceEncoding match {
      case Some(enc) => Source.fromFile(mfile.source, enc)
      case None      => Source.fromFile(mfile.source, Codec.UTF8.name)
    }
    try src.mkString
    finally src.close()
  }

  private def spanStart(status: StatementStatus): String =
    s"<span style='${cellStyle(status)}'>"

  private def cellStyle(status: StatementStatus): String = {
    val GREEN = "#AEF1AE"
    val RED = "#F0ADAD"
    status match {
      case Invoked    => s"background: $GREEN"
      case NotInvoked => s"background: $RED"
      case NoData     => ""
    }
  }
}
