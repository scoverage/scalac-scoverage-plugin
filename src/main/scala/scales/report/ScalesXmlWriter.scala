package scales.report

import scales.{IOUtils, MeasuredStatement, Coverage}

/** @author Stephen Samuel */
object ScalesXmlWriter extends ScalesWriter {

  def write(coverage: Coverage) = {
    val statements = statements2xml(coverage.statements)
    val xml = <scales timestamp={System.currentTimeMillis.toString}>
      {statements}
    </scales>
    IOUtils.write("scales.xml", xml.toString())
  }

  def statements2xml(statements: Iterable[MeasuredStatement]) = statements.map(arg => statement2xml(arg))
  def statement2xml(statement: MeasuredStatement) =
    <statement source={statement.source.path}
               package={statement.location._package}
               class={statement.location._class}
               method={statement.location.method.orNull}
               start={statement.start.toString}
               line={statement.line.toString}
               count={statement.count.toString}>
      {statement.desc}
    </statement>
}
