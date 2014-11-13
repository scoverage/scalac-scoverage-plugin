package scoverage.report

import java.io.File

import _root_.scoverage.{Coverage, MeasuredClass, MeasuredMethod, MeasuredPackage}
import org.apache.commons.io.FileUtils

import scala.xml.Node

/** @author Stephen Samuel */
class CoberturaXmlWriter(baseDir: File, outputDir: File) {

  def format(double: Double): String = "%.2f".format(double)

  def write(coverage: Coverage): Unit = {
    FileUtils.write(new File(outputDir.getAbsolutePath + "/cobertura.xml"),
      "<?xml version=\"1.0\"?>\n<!DOCTYPE coverage SYSTEM \"http://cobertura.sourceforge.net/xml/coverage-04.dtd\">\n" +
        xml(coverage))
  }

  def method(method: MeasuredMethod): Node = {
    <method name={method.name}
            signature="()V"
            line-rate={format(method.statementCoverage)}
            branch-rate={format(method.branchCoverage)}>
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
           filename={
            val absPath = baseDir.getAbsolutePath.last == File.separatorChar match {
              case true => baseDir.getAbsolutePath
              case false => baseDir.getAbsolutePath + File.separatorChar
            }
            klass.source.replace(absPath, "")}
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
        <source>/src/main/scala</source>
      </sources>
      <packages>
        {coverage.packages.map(pack)}
      </packages>
    </coverage>
  }
}
