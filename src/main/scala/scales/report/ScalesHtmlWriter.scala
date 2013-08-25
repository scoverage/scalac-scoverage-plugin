package scales.report

import scales._
import scala.reflect.internal.util.SourceFile
import scala.xml.{Unparsed, Node}
import scales.MeasuredFile
import java.util.Date

/** @author Stephen Samuel */
object ScalesHtmlWriter extends ScalesWriter {

  def write(coverage: Coverage) {
    writeIndex(coverage)
    for ( file <- coverage.files ) {
      val data = html(file)
      IOUtils.write(file.source.file.path + ".html", data)
    }
  }

  def risks(coverage: Coverage) = {
    <div id="risks">
      <div>Total 20 Project Risks</div>{coverage.risks(20).map(arg => <div>
      {arg.name}
    </div>)}
    </div>
  }

  def packages(coverage: Coverage) = {
    val rows = coverage.packages.map(arg => {
      <tr>
        <td>
          {arg.name}
        </td>
        <td>
          {arg.invokedClasses.toString}
          /
          {arg.classCount}
          (
          {arg.classCoverage.toString}
          %)
        </td>
        <td>
          {arg.invokedStatements.toString}
          /
          {arg.statementCount}
          (
          {arg.statementCoverage.toString}
          %)
        </td>
      </tr>
    })
    <table>
      {rows}
    </table>
  }

  def overview(coverage: Coverage) = {
    <table>
      <caption>Statistics generated at
        {new Date().toString}
      </caption>
      <tr>
        <td>Lines of code:</td>
        <td>
          {coverage.loc.toString}
        </td>
        <td>Statements:</td>
        <td>
          {coverage.statementCount.toString}
        </td>
        <td>Clases per package:</td>
        <td>
          {coverage.classesPerPackage.toString}
        </td>
        <td>Methods per class:</td>
        <td>
          {coverage.methodsPerClass.toString}
        </td>
      </tr>
      <tr>
        <td>Non comment lines of code:</td>
        <td>
          {coverage.ncloc.toString}
        </td>
        <td>Packages:</td>
        <td>
          {coverage.classCount.toString}
        </td>
        <td>Classes:</td>
        <td>
          {coverage.packageCount.toString}
        </td>
        <td>Methods:</td>
        <td>
          {coverage.methodCount.toString}
        </td>
      </tr>
    </table>
  }

  def writeIndex(coverage: Coverage) {
    val data = <html>
      <head>
        <title>Scales Code Coverage Overview</title>
        <link rel="stylesheet" href="http://yui.yahooapis.com/pure/0.2.0/pure-nr-min.css"/>
      </head>
      <body>
        <h1>Scales Code Coverage</h1>{overview(coverage)}{risks(coverage)}{packages(coverage)}
      </body>
    </html>
    IOUtils.write("index.html", data.toString())
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
      <link rel="stylesheet" href="http://yui.yahooapis.com/pure/0.2.0/pure-nr-min.css"/>
    </head>
    <body>
      <h1>
        Filename:
        {file.source}
      </h1>
      <div>Statement Coverage:
        {file.invokedStatements.toString}
        /
        {file.statementCount.toString}{file.statementCoverage.toString}
        %
      </div>
      <table>
        {table(file)}
      </table>
    </body>
  </html>
}

