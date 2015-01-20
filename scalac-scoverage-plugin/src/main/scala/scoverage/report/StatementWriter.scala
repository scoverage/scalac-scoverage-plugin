package scoverage.report

import _root_.scoverage.MeasuredFile

import scala.xml.Node

/** @author Stephen Samuel */
class StatementWriter(mFile: MeasuredFile) {

  val GREEN = "#AEF1AE"
  val RED = "#F0ADAD"

  def output: Node = {

    def cellStyle(invoked: Boolean): String = invoked match {
      case true => s"background: $GREEN"
      case false => s"background: $RED"
    }

    <table cellspacing="0" cellpadding="0" class="table statementlist">
      <tr>
        <th>Line</th>
        <th>Stmt Id</th>
        <th>Pos</th>
        <th>Tree</th>
        <th>Symbol</th>
        <th>Code</th>
      </tr>{mFile.statements.toSeq.sortBy(_.line).map(stmt => {
      <tr>
        <td>
          {stmt.line}
        </td>
        <td>
          {stmt.id}
        </td>
        <td>
          {stmt.start.toString}
          -
          {stmt.end.toString}
        </td>
        <td>
          {stmt.treeName}
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
