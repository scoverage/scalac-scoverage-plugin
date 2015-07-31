package scoverage.report

import java.io.File

import scoverage._

import scala.xml.{Node, PrettyPrinter}

/** @author Stephen Samuel */
class CoberturaXmlWriter(sourceDirectories: Seq[File], outputDir: File) extends BaseReportWriter(sourceDirectories, outputDir) {

  def this (baseDir: File, outputDir: File) {
    this(Seq(baseDir), outputDir)
  }
  
  def format(double: Double): String = "%.2f".format(double)

  def write(coverage: Coverage): Unit = {
    val file = new File(outputDir, "cobertura.xml")
    IOUtils.writeToFile(file, "<?xml version=\"1.0\"?>\n<!DOCTYPE coverage SYSTEM \"https://raw.githubusercontent.com/cobertura/cobertura/master/cobertura/src/site/htdocs/xml/coverage-04.dtd\">\n" + 
        new PrettyPrinter(120, 4).format(xml(coverage)))
  }

  def method(method: MeasuredMethod): Node = {
    <method name={method.name}
            signature="()V"
            line-rate={format(method.statementCoverage)}
            branch-rate={format(method.branchCoverage)}
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
    <class name={klass.name}
           filename={relativeSource(klass.source).replace(File.separator, "/")}
           line-rate={format(klass.statementCoverage)}
           branch-rate={format(klass.branchCoverage)}
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
             line-rate={format(pack.statementCoverage)}
             branch-rate={format(pack.branchCoverage)}
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
    <coverage line-rate={format(coverage.statementCoverage)}
              lines-covered={coverage.statementCount.toString}
              lines-valid={coverage.invokedStatementCount.toString}
              branches-covered={coverage.branchCount.toString}
              branches-valid={coverage.invokedBranchesCount.toString}
              branch-rate={format(coverage.branchCoverage)}
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
