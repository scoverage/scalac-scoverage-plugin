package scoverage.report

import scala.xml.{PrettyPrinter, Node}
import java.io.File
import org.apache.commons.io.FileUtils
import scoverage._
import scoverage.MeasuredStatement
import scoverage.MeasuredClass
import scoverage.MeasuredMethod

/** @author Stephen Samuel */
class ScoverageXmlWriter(sourceDir: File, outputDir: File) {

  def write(coverage: Coverage): Unit = {
    FileUtils.write(
      new File(outputDir.getAbsolutePath + "/scoverage.xml"),
      new PrettyPrinter(120, 4).format(xml(coverage))
    )
  }

  def statement(stmt: MeasuredStatement): Node = {
    <statement package={stmt.location._package}
               class={stmt.location._class}
               method={stmt.location.method}
               start={stmt.start.toString}
               line={stmt.line.toString}
               symbol={stmt.symbolName}
               tree={stmt.treeName}
               branch={stmt.branch.toString}
               invocation-count={stmt.count.toString}>
      {stmt.desc}
    </statement>
  }

  def method(method: MeasuredMethod): Node = {
    <method name={method.name}
            statement-count={method.statementCount.toString}
            statements-invoked={method.invokedStatementCount.toString}
            statement-rate={method.statementCoverageFormatted}
            branch-rate={method.branchCoverageFormatted}>
      <statements>
        {method.statements.map(statement)}
      </statements>
    </method>
  }

  def klass(klass: MeasuredClass): Node = {
    <class name={klass.name}
           filename={klass.source.replace(sourceDir.getAbsolutePath, "")}
           statement-count={klass.statementCount.toString}
           statements-invoked={klass.invokedStatementCount.toString}
           statement-rate={klass.statementCoverageFormatted}
           branch-rate={klass.branchCoverageFormatted}>
      <methods>
        {klass.methods.map(method)}
      </methods>
    </class>
  }

  def pack(pack: MeasuredPackage): Node = {
    <package name={pack.name}
             statement-count={pack.statementCount.toString}
             statements-invoked={pack.invokedStatementCount.toString}
             statement-rate={pack.statementCoverageFormatted}>
      <classes>
        {pack.classes.map(klass)}
      </classes>
    </package>
  }

  def xml(coverage: Coverage): Node = {
    <scoverage statement-count={coverage.statementCount.toString}
               statements-invoked={coverage.invokedStatementCount.toString}
               statement-rate={coverage.statementCoverageFormatted}
               branch-rate={coverage.branchCoverageFormatted}
               version="1.0"
               timestamp={System.currentTimeMillis.toString}>
      <packages>
        {coverage.packages.map(pack)}
      </packages>
    </scoverage>
  }
}

