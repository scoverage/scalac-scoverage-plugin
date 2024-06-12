package scoverage.reporter

import java.io.File

import scala.xml.Node
import scala.xml.PrettyPrinter

import scoverage.domain.Coverage
import scoverage.domain.DoubleFormat
import scoverage.domain.MeasuredClass
import scoverage.domain.MeasuredMethod
import scoverage.domain.MeasuredPackage

/** @author Stephen Samuel */
class CoberturaXmlWriter(
    sourceDirectories: Seq[File],
    outputDir: File,
    sourceEncoding: Option[String],
    recoverNoSourceRoot: BaseReportWriter.PathRecoverer
) extends BaseReportWriter(
      sourceDirectories,
      outputDir,
      sourceEncoding,
      recoverNoSourceRoot
    ) {

  def this(
      baseDir: File,
      outputDir: File,
      sourceEncoding: Option[String],
      recoverNoSourceRoot: BaseReportWriter.PathRecoverer
  ) = {
    this(Seq(baseDir), outputDir, sourceEncoding, recoverNoSourceRoot)
  }

  def write(coverage: Coverage): Unit = {
    val file = new File(outputDir, "cobertura.xml")
    IOUtils.writeToFile(
      file,
      "<?xml version=\"1.0\"?>\n<!DOCTYPE coverage SYSTEM \"https://cobertura.sourceforge.net/xml/coverage-04.dtd\">\n" +
        new PrettyPrinter(120, 4).format(xml(coverage)),
      sourceEncoding
    )
  }

  def method(method: MeasuredMethod): Node = {
    <method name={method.name}
            signature="()V"
            line-rate={DoubleFormat.twoFractionDigits(method.statementCoverage)}
            branch-rate={DoubleFormat.twoFractionDigits(method.branchCoverage)}
            complexity="0">
      <lines>
        {
          method.statements.map(stmt => <line
          number={stmt.line.toString}
          hits={stmt.count.toString}
          branch={stmt.branch.toString}/>)
        }
      </lines>
    </method>
  }

  def klass(klass: MeasuredClass): Option[Node] = {
    relativeSource(klass.source).map(sourcePath => {
      <class name={klass.fullClassName}
             filename={sourcePath}
             line-rate={DoubleFormat.twoFractionDigits(klass.statementCoverage)}
             branch-rate={DoubleFormat.twoFractionDigits(klass.branchCoverage)}
             complexity="0">
        <methods>
          {klass.methods.map(method)}
        </methods>
        <lines>
          {
            klass.statements.map(stmt => <line
            number={stmt.line.toString}
            hits={stmt.count.toString}
            branch={stmt.branch.toString}/>)
          }
        </lines>
      </class>
    })
  }

  def pack(pack: MeasuredPackage): Node = {
    <package name={pack.name}
             line-rate={DoubleFormat.twoFractionDigits(pack.statementCoverage)}
             branch-rate={DoubleFormat.twoFractionDigits(pack.branchCoverage)}
             complexity="0">
      <classes>
        {pack.classes.flatMap(klass)}
      </classes>
    </package>
  }

  def source(src: File): Node = {
    <source>{src.getCanonicalPath.replace(File.separator, "/")}</source>
  }

  def xml(coverage: Coverage): Node = {
    <coverage line-rate={
      DoubleFormat.twoFractionDigits(coverage.statementCoverage)
    }
              lines-valid={coverage.statementCount.toString}
              lines-covered={coverage.invokedStatementCount.toString}
              branches-valid={coverage.branchCount.toString}
              branches-covered={coverage.invokedBranchesCount.toString}
              branch-rate={
                DoubleFormat.twoFractionDigits(coverage.branchCoverage)
              }
              complexity="0"
              version="1.0"
              timestamp={System.currentTimeMillis.toString}>
      <sources>
        <source>--source</source>
        {sourceDirectories.filter(_.isDirectory).map(source)}
      </sources>
      <packages>
        {coverage.packages.map(pack)}
      </packages>
    </coverage>
  }

}
