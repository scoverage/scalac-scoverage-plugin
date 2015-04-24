package scoverage.report

import java.io.File

import scoverage._

import scala.xml.{Node, PrettyPrinter}

/** @author Stephen Samuel */
class ScoverageXmlWriter(sourceDirectories: Seq[File], outputDir: File, debug: Boolean) extends BaseReportWriter(sourceDirectories, outputDir) {

  def this (sourceDir: File, outputDir: File, debug: Boolean) {
    this(Seq(sourceDir), outputDir, debug);
  }

  def write(coverage: Coverage): Unit = {
    val file = IOUtils.reportFile(outputDir, debug)
    IOUtils.writeToFile(file, new PrettyPrinter(120, 4).format(xml(coverage)))
  }

  private def xml(coverage: Coverage): Node = {
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

  private def statement(stmt: Statement): Node = {
    debug match {
      case true =>
        <statement package={stmt.location.packageName}
                   class={stmt.location.className}
                   class-type={stmt.location.classType.toString}
                   top-level-class={stmt.location.topLevelClass}
                   source={stmt.source}
                   method={stmt.location.method}
                   start={stmt.start.toString}
                   end={stmt.end.toString}
                   line={stmt.line.toString}
                   symbol={Serializer.escape(stmt.symbolName)}
                   tree={Serializer.escape(stmt.treeName)}
                   branch={stmt.branch.toString}
                   invocation-count={stmt.count.toString}
                   ignored={stmt.ignored.toString}>
          {Serializer.escape(stmt.desc)}
        </statement>
      case false =>
          <statement package={stmt.location.packageName}
                     class={stmt.location.className}
                     class-type={stmt.location.classType.toString}
                     top-level-class={stmt.location.topLevelClass}
                     source={stmt.source}
                     method={stmt.location.method}
                     start={stmt.start.toString}
                     end={stmt.end.toString}
                     line={stmt.line.toString}
                     branch={stmt.branch.toString}
                     invocation-count={stmt.count.toString}
                     ignored={stmt.ignored.toString}/>
    }
  }

  private def method(method: MeasuredMethod): Node = {
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

  private def klass(klass: MeasuredClass): Node = {
    <class name={klass.name}
           filename={relativeSource(klass.source)}
           statement-count={klass.statementCount.toString}
           statements-invoked={klass.invokedStatementCount.toString}
           statement-rate={klass.statementCoverageFormatted}
           branch-rate={klass.branchCoverageFormatted}>
      <methods>
        {klass.methods.map(method)}
      </methods>
    </class>
  }

  private def pack(pack: MeasuredPackage): Node = {
    <package name={pack.name}
             statement-count={pack.statementCount.toString}
             statements-invoked={pack.invokedStatementCount.toString}
             statement-rate={pack.statementCoverageFormatted}>
      <classes>
        {pack.classes.map(klass)}
      </classes>
    </package>
  }

}
