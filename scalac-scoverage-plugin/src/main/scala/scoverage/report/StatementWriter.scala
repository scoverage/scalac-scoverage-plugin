package scoverage.report

import _root_.scoverage.MeasuredFile

import scala.xml.Node

/** @author Stephen Samuel */
class StatementWriter(mfile: MeasuredFile) {

  val GREEN = "#AEF1AE"
  val RED = "#F0ADAD"

  def output: Node = {

    def cellStyle(invoked: Boolean): String = invoked match {
      case true => s"background: $GREEN"
      case false => s"background: $RED"
    }

    <table cellspacing="0" cellpadding="0" class="table statementlist">
      <tr>
        <th>Line Number</th>
        <th>Statement Id</th>
        <th>Symbol</th>
        <th>Code</th>
      </tr>
      {mfile.statements.toSeq.sortBy(_.line).map(stmt => {
      <tr>
        <td>
          {stmt.line}
        </td>
        <td>
          {stmt.id}
        </td>
        <td>
          {stmt.symbolName}
        </td>
        <td style={cellStyle(stmt.isInvoked)}>
          {stmt.desc}
        </td>
      </tr>
    })}
    </table>
  }
}
