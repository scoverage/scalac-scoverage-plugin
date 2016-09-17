package scoverage.report

import java.io.File

import scoverage.DoubleFormat.twoFractionDigits
import scoverage._

import scala.xml.{Node, PrettyPrinter}

/** @author Stephen Samuel */
class CoberturaXmlWriter(sourceDirectories: Seq[File], outputDir: File) extends BaseReportWriter(sourceDirectories, outputDir) {

  def this (baseDir: File, outputDir: File) {
    this(Seq(baseDir), outputDir)
  }

  def write(coverage: Coverage): Unit = {
    val file = new File(outputDir, "cobertura.xml")
    IOUtils.writeToFile(file, "<?xml version=\"1.0\"?>\n<!DOCTYPE coverage SYSTEM \"http://cobertura.sourceforge.net/xml/coverage-04.dtd\">\n" +
        new PrettyPrinter(120, 4).format(xml(coverage)))
  }

  def method(method: MeasuredMethod): Node = {
    <method name={method.name}
            signature="()V"
            line-rate={twoFractionDigits(method.statementCoverage)}
            branch-rate={twoFractionDigits(method.branchCoverage)}
            complexity="0">
      <lines>
        {method.statements.map(stmt =>
          <line
          number={stmt.line.toString}
          hits={stmt.count.toString}
          branch="false"/>
      )}
      </lines>
    </method>
  }

  def klass(klass: MeasuredClass): Node = {
    <class name={klass.fullClassName}
           filename={relativeSource(klass.source).replace(File.separator, "/")}
           line-rate={twoFractionDigits(klass.statementCoverage)}
           branch-rate={twoFractionDigits(klass.branchCoverage)}
           complexity="0">
      <methods>
        {klass.methods.map(method)}
      </methods>
      <lines>
        {klass.statements.map(stmt =>
          <line
          number={stmt.line.toString}
          hits={stmt.count.toString}
          branch="false"/>
      )}
      </lines>
    </class>
  }

  def pack(pack: MeasuredPackage): Node = {
    <package name={pack.name}
             line-rate={twoFractionDigits(pack.statementCoverage)}
             branch-rate={twoFractionDigits(pack.branchCoverage)}
             complexity="0">
      <classes>
        {pack.classes.map(klass)}
      </classes>
    </package>
  }

  def source(src: File): Node = {
    <source>{src.getCanonicalPath.replace(File.separator, "/")}</source>
  }

  def xml(coverage: Coverage): Node = {
    <coverage line-rate={twoFractionDigits(coverage.statementCoverage)}
              lines-valid={coverage.statementCount.toString}
              lines-covered={coverage.invokedStatementCount.toString}
              branches-valid={coverage.branchCount.toString}
              branches-covered={coverage.invokedBranchesCount.toString}
              branch-rate={twoFractionDigits(coverage.branchCoverage)}
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
