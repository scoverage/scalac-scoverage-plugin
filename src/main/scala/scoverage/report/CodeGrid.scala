package scoverage.report

import scoverage.{MeasuredFile, MeasuredStatement}
import org.apache.commons.io.IOUtils
import java.io.{File, FileInputStream}
import scala.xml.{Unparsed, Node}

/** @author Stephen Samuel */
class CodeGrid(mfile: MeasuredFile) {
  case class Cell(char: Char, var status: StatementStatus)

  val sep = System.getProperty("line.separator").charAt(0)

  // note: we must reinclude the line sep to keep source positions correct.
  val lines = source(mfile).split(sep).map(line => (line.toCharArray :+ '\n').map(Cell(_, NoData)))
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
        <td style={cellStyle(cell.status)}>
          {Unparsed(cell.char.toString.replace(" ", "&nbsp;"))}
        </td>
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

