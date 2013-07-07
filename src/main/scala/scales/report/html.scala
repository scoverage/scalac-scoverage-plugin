package scales.report

import scales._
import java.io.{FileWriter, BufferedWriter, File}
import scala.reflect.internal.util.SourceFile
import scala.xml.{Unparsed, Node}
import scales.MeasuredFile

/** @author Stephen Samuel */
object ScalesHtmlWriter extends CoverageWriter {
    def write(coverage: Coverage) {
        for ( file <- coverage.files ) {
            val data = html(file)
            write(file.source.path.replace(".scala", "") + ".html", data)
        }
    }

    def write(path: String, data: AnyRef) {
        println(s"Writing to path $path")
        val file = new File(path)
        val writer = new BufferedWriter(new FileWriter(file))
        writer.write(data.toString)
        writer.close()
    }

    def lines(source: SourceFile): Seq[String] = new String(source.content).split("\n")
    def formatLine(line: String) = line.replace(" ", "&nbsp;").replace("\t", "&nbsp;&nbsp;&nbsp;")

    def table(file: MeasuredFile): Seq[Node] = {
        var lineNumber = 0
        lines(file.source).map(line => {
            lineNumber = lineNumber + 1
            val status = file.lineStatus(lineNumber)
            val css = lineCss(status)
            <tr>
                <td>
                    {lineNumber.toString}
                </td>
                <td style={css}>
                    {Unparsed(formatLine(line))}
                </td>
            </tr>
        })
    }

    def lineCss(status: LineStatus): String = status match {
        case Covered => "background: green"
        case MissingCoverage => "background: red"
        case NotInstrumented => "background: white"
    }

    def html(file: MeasuredFile) = <html>
        <head>
            <title>
                {file.source}
            </title>
        </head>
        <body>
            <h1>
                Filename:
                {file.source}
            </h1>
            <div>Statement Coverage:
                {file.invokedStatements.toString}
                /
                {file.totalStatements.toString}{file.statementCoverage.toString}
                %
            </div>
            <table>
                {table(file)}
            </table>
        </body>
    </html>
}

