package scoverage.reporter

import java.io.File

import scala.xml.Node
import scala.xml.PrettyPrinter

import scoverage.domain.Coverage
import scoverage.domain.MeasuredClass
import scoverage.domain.MeasuredMethod
import scoverage.domain.MeasuredPackage
import scoverage.domain.Statement

/** @author Stephen Samuel */
class ScoverageXmlWriter(
    sourceDirectories: Seq[File],
    outputDir: File,
    debug: Boolean,
    sourceEncoding: Option[String]
) extends BaseReportWriter(sourceDirectories, outputDir, sourceEncoding) {

  def this(
      sourceDir: File,
      outputDir: File,
      debug: Boolean,
      sourceEncoding: Option[String]
  ) = {
    this(Seq(sourceDir), outputDir, debug, sourceEncoding)
  }

  def write(coverage: Coverage): Unit = {
    val file = IOUtils.reportFile(outputDir, debug)
    IOUtils.writeToFile(
      file,
      new PrettyPrinter(120, 4).format(xml(coverage)),
      sourceEncoding
    )
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
                   full-class-name={stmt.location.fullClassName}
                   source={stmt.source}
                   method={stmt.location.method}
                   start={stmt.start.toString}
                   end={stmt.end.toString}
                   line={stmt.line.toString}
                   symbol={escape(stmt.symbolName)}
                   tree={escape(stmt.treeName)}
                   branch={stmt.branch.toString}
                   invocation-count={stmt.count.toString}
                   ignored={stmt.ignored.toString}>
          {escape(stmt.desc)}
        </statement>
      case false =>
        <statement package={stmt.location.packageName}
                     class={stmt.location.className}
                     class-type={stmt.location.classType.toString}
                     full-class-name={stmt.location.fullClassName}
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
    <class name={klass.fullClassName}
           filename={klass.source}
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

  /** This method ensures that the output String has only
    * valid XML unicode characters as specified by the
    * XML 1.0 standard. For reference, please see
    * <a href="http://www.w3.org/TR/2000/REC-xml-20001006#NT-Char">the
    * standard</a>. This method will return an empty
    * String if the input is null or empty.
    *
    * @param in The String whose non-valid characters we want to remove.
    * @return The in String, stripped of non-valid characters.
    * @see http://blog.mark-mclaren.info/2007/02/invalid-xml-characters-when-valid-utf8_5873.html
    */
  def escape(in: String): String = {
    val out = new StringBuilder()
    for (current <- Option(in).getOrElse("").toCharArray) {
      if (
        (current == 0x9) || (current == 0xa) || (current == 0xd) ||
        ((current >= 0x20) && (current <= 0xd7ff)) ||
        ((current >= 0xe000) && (current <= 0xfffd)) ||
        ((current >= 0x10000) && (current <= 0x10ffff))
      )
        out.append(current)
    }
    out.mkString
  }

}
