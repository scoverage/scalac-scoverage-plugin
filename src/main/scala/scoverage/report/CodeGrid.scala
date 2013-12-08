package scoverage.report

import scoverage.{MeasuredFile, MeasuredStatement}
import org.apache.commons.io.IOUtils
import java.io.{File, FileInputStream}
import scala.xml.{Unparsed, Node}

/** @author Stephen Samuel */
class CodeGrid(mfile: MeasuredFile) {
  case class Cell(char: Char, var status: StatementStatus)

  val GREEN = "#AEF1AE"
  val RED = "#F0ADAD"

  val sep = System.getProperty("line.separator").charAt(0)

  // note: we must reinclude the line sep to keep source positions correct.
  val lines = source(mfile).split(sep).map(line => (line.toCharArray :+ '\n').map(Cell(_, NotInstrumented)))
  val cells = lines.flatten

  mfile.statements.foreach(highlight)

  def source(mfile: MeasuredFile): String = IOUtils.toString(new FileInputStream(new File(mfile.source)), "UTF-8")

  def highlight(stmt: MeasuredStatement) {
    for ( k <- stmt.start until stmt.end ) {
      if (k < cells.size)
        if (cells(k).status != NotInvoked) {
          if (stmt.isInvoked)
            cells(k).status = Invoked
          else
            cells(k).status = NotInvoked
        }
    }
  }

  val css = "table.codegrid { font-family: Courier; font-size: 12px } " +
    "table.linenumber { width: 40px } "

  def output: Node = {
    var lineNumber = 0
    <html>
      <head>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
        <title id='title'>
          {mfile.source}
        </title>
        <link rel="stylesheet" href="//netdna.bootstrapcdn.com/bootstrap/3.0.3/css/bootstrap.min.css"/>
        <script src="//netdna.bootstrapcdn.com/bootstrap/3.0.3/js/bootstrap.min.js"></script>
        <style>
          {css}
        </style>
      </head>
      <body style="font-family: monospace;">
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
        <br/>
        <br/>
        <br/>
        <br/>
      </body>
    </html>
  }

  def cellStyle(status: StatementStatus): String = status match {
    case Invoked => s"background: $GREEN"
    case NotInvoked => s"background: $RED"
    case NotInstrumented => "background: white"
  }
}

