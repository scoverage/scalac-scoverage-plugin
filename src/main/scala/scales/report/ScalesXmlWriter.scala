package scales.report

import scales.{MeasuredStatement, Coverage}
import java.io.{FileWriter, BufferedWriter, File}

/** @author Stephen Samuel */
object ScalesXmlWriter extends ScalesWriter {

    def write(coverage: Coverage) = {
        val statements = statements2xml(coverage.statements)
        val xml = <scales timestamp={System.currentTimeMillis.toString}>
            {statements}
        </scales>
        write("scales.xml", xml.toString())
    }

    def write(path: String, data: AnyRef) {
        println(s"Writing to path $path")
        val file = new File(path)
        val writer = new BufferedWriter(new FileWriter(file))
        writer.write(data.toString)
        writer.close()
    }

    def statements2xml(statements: Iterable[MeasuredStatement]) = statements.map(arg => statement2xml(arg))
    def statement2xml(statement: MeasuredStatement) =
        <statement source={statement.source.path} package={statement._package} class={statement._class} method={statement._method}
                   start={statement.start.toString} line={statement.line.toString} count={statement.count.toString}>
            {statement.desc}
        </statement>
}
