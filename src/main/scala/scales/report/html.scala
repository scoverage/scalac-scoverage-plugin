package scales.report

import scales.Coverage
import java.io.{FileWriter, BufferedWriter, File}
import scala.reflect.internal.util.SourceFile

/** @author Stephen Samuel */
object ScalesHtmlWriter extends CoverageWriter {
    def write(coverage: Coverage) {
        for ( file <- coverage.files ) {
            val data = html(file.source)
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

    def lines(source: SourceFile) = new String(source.content)

    def html(source: SourceFile) = <html>
        <head>
            <title>
                {source}
            </title>
        </head>
        <body>
            <h1>
                {source}
            </h1>{lines(source)}
        </body>
    </html>
}

