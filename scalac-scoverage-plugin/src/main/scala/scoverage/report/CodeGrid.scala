package scoverage.report

import java.io.{File, FileInputStream}

import _root_.scoverage.{MeasuredFile, MeasuredStatement}
import org.apache.commons.io.IOUtils

import scala.xml.{Node, Unparsed}

/** @author Stephen Samuel */
class CodeGrid(mfile: MeasuredFile) {
  case class Cell(char: Char, var status: StatementStatus)

  /**
   * Regardless of whether the source is Unix (\n) or DOS (\r\n), the lines will end
   * with \n. We split on \n and allow an optional trailing \r on the line.
   * This lets us split on lines while keep the source positions matching up.
   */
  val lineBreak = '\n'

  // note: we must reinclude the line sep to keep source positions correct.
  val lines = source(mfile).split(lineBreak).map(line => (line.toCharArray :+ lineBreak).map(Cell(_, NoData)))
  val cells = lines.flatten

  mfile.statements.foreach(highlight)

  def source(mfile: MeasuredFile): String = IOUtils.toString(new FileInputStream(new File(mfile.source)), "UTF-8")

  def highlight(stmt: MeasuredStatement) {
    // notinvoked is a stronger property than invoked
    for ( k <- stmt.start until stmt.end ) {
      if (k < cells.size)
        if (!stmt.isInvoked) {
          cells(k).status = NotInvoked
        } else if (cells(k).status == NoData) {
          cells(k).status = Invoked
        }
    }
  }

  def output: Node = {
    var lineNumber = 0
    <table cellspacing="0" cellpadding="0" class="table codegrid">
      {lines.map(line => {
      lineNumber = lineNumber + 1
      <tr>
        <td class="linenumber">
          {lineNumber.toString}
        </td>{line.map(cell => {
        // don't need to output the final \n but don't ever exclude from cells
        if (cell.char != '\n') {
          <td style={cellStyle(cell.status)}>
            {Unparsed(cell.char.toString.replace(" ", "&nbsp;"))}
          </td>
        }
      })}
      </tr>
    })}
    </table>

  }

  val GREEN = "#AEF1AE"
  val RED = "#F0ADAD"

  private def cellStyle(status: StatementStatus): String = status match {
    case Invoked => s"background: $GREEN"
    case NotInvoked => s"background: $RED"
    case NoData => "background: white"
  }
}

