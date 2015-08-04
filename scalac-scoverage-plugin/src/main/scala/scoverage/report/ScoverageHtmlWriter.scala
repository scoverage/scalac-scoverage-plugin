package scoverage.report

import java.io.File
import java.util.Date

import scoverage._

import scala.xml.Node

/** @author Stephen Samuel */
class ScoverageHtmlWriter(sourceDirectories: Seq[File], outputDir: File) extends BaseReportWriter(sourceDirectories, outputDir) {

  def this (sourceDirectory: File, outputDir: File) {
    this(Seq(sourceDirectory), outputDir);
  }
  
  def write(coverage: Coverage): Unit = {
    val indexFile = new File(outputDir.getAbsolutePath + "/index.html")
    val packageFile = new File(outputDir.getAbsolutePath + "/packages.html")
    val overviewFile = new File(outputDir.getAbsolutePath + "/overview.html")

    val index = IOUtils.readStreamAsString(getClass.getResourceAsStream("/scoverage/index.html"))
    IOUtils.writeToFile(indexFile, index)
    IOUtils.writeToFile(packageFile, packageList(coverage).toString())
    IOUtils.writeToFile(overviewFile, overview(coverage).toString())

    coverage.packages.foreach(writePackage)
  }

  private def writePackage(pkg: MeasuredPackage): Unit = {
    // package overview files are written out using a filename that respects the package name
    // that means package com.example declared in a class at src/main/scala/mystuff/MyClass.scala will be written
    // to com.example.html
    val file = new File(outputDir, packageOverviewRelativePath(pkg))
    file.getParentFile.mkdirs()
    IOUtils.writeToFile(file, packageOverview(pkg).toString())
    pkg.files.foreach(writeFile)
  }

  private def writeFile(mfile: MeasuredFile): Unit = {
    // each highlighted file is written out using the same structure as the original file.
    val file = new File(outputDir, relativeSource(mfile.source) + ".html")
    file.getParentFile.mkdirs()
    IOUtils.writeToFile(file, filePage(mfile).toString())
  }

  private def packageOverviewRelativePath(pkg: MeasuredPackage) = pkg.name.replace("<empty>", "(empty)") + ".html"

  private def filePage(mfile: MeasuredFile): Node = {
    val filename = relativeSource(mfile.source) + ".html"
    val css =
      "table.codegrid { font-family: monospace; font-size: 12px; width: auto!important; }" +
        "table.statementlist { width: auto!important; font-size: 13px; } " +
        "table.codegrid td { padding: 0!important; border: 0!important } " +
        "table td.linenumber { width: 40px!important; } "
    <html>
      <head>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
        <title id='title'>
          {filename}
        </title>
        {plugins}
        <style>
          {css}
        </style>
      </head>
      <body style="font-family: monospace;">
        <ul class="nav nav-tabs">
          <li>
            <a href="#codegrid" data-toggle="tab">Codegrid</a>
          </li>
          <li>
            <a href="#statementlist" data-toggle="tab">Statement List</a>
          </li>
        </ul>
        <div class="tab-content">
          <div class="tab-pane active" id="codegrid">
            {xml.Unparsed(new CodeGrid(mfile).highlighted)}
          </div>
          <div class="tab-pane" id="statementlist">
            {new StatementWriter(mfile).output}
          </div>
        </div>
      </body>
    </html>

  }

  def header = {
    val css = """.meter {
                |        height: 14px;
                |        position: relative;
                |        background: #BB2020;
                |}
                |
                |.meter span {
                |	display: block;
                |	height: 100%;
                |	background-color: rgb(43,194,83);
                |	background-image: -webkit-gradient(
                |	  linear,
                |	  left bottom,
                |	  left top,
                |	  color-stop(0, rgb(43,194,83)),
                |	  color-stop(1, rgb(84,240,84))
                |	 );
                |	background-image: -webkit-linear-gradient(
                |	  center bottom,
                |	  rgb(43,194,83) 37%,
                |	  rgb(84,240,84) 69%
                |	 );
                |	background-image: -moz-linear-gradient(
                |	  center bottom,
                |	  rgb(43,194,83) 37%,
                |	  rgb(84,240,84) 69%
                |	 );
                |	background-image: -ms-linear-gradient(
                |	  center bottom,
                |	  rgb(43,194,83) 37%,
                |	  rgb(84,240,84) 69%
                |	 );
                |	background-image: -o-linear-gradient(
                |	  center bottom,
                |	  rgb(43,194,83) 37%,
                |	  rgb(84,240,84) 69%
                |	 );
                |	-webkit-box-shadow:
                |	  inset 0 2px 9px  rgba(255,255,255,0.3),
                |	  inset 0 -2px 6px rgba(0,0,0,0.4);
                |	-moz-box-shadow:
                |	  inset 0 2px 9px  rgba(255,255,255,0.3),
                |	  inset 0 -2px 6px rgba(0,0,0,0.4);
                |	position: relative;
                |	overflow: hidden;
                |}""".stripMargin

    <head>
      <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
      <title id='title'>Scoverage Code Coverage</title>
      {plugins}
      <style>
        {css}
      </style>
    </head>
  }

  def packageOverview(pack: MeasuredPackage): Node = {
    <html>
      {header}<body style="font-family: monospace;">
      {classesTable(pack.classes, addPath = false)}
    </body>
    </html>
  }

  def classesTable(classes: Iterable[MeasuredClass], addPath: Boolean): Node = {
    <table class="tablesorter table table-striped" style="font-size:13px">
      <thead>
        <tr>
          <th>
            Class
          </th>
          <th>
            Source file
          </th>
          <th>
            Lines
          </th>
          <th>
            Methods
          </th>
          <th>
            Statements
          </th>
          <th>
            Invoked
          </th>
          <th>
            Coverage
          </th>
          <th>
          </th>
          <th>
            Branches
          </th>
          <th>
            Invoked
          </th>
          <th>
            Coverage
          </th>
          <th>
          </th>
        </tr>
      </thead>
      <tbody>
        {classes.toSeq.sortBy(_.simpleName) map classRow}
      </tbody>
    </table>
  }

  def classRow(klass: MeasuredClass): Node = {

    val filename: String = {

      val fileRelativeToSource = new File(relativeSource(klass.source) + ".html")
      val path = fileRelativeToSource.getParent
      val value = fileRelativeToSource.getName

      if (path.ne("")) {
        // (Normalise the pathSeparator to "/" in case we are running on Windows)
        fileRelativeToSource.toString.replace(File.separator, "/")
      } else {
        value
      }
    }

    val statement0f = Math.round(klass.statementCoveragePercent).toInt.toString
    val branch0f = Math.round(klass.branchCoveragePercent).toInt.toString

    val simpleClassName = klass.name.split('.').last
    <tr>
      <td>
        <a href={filename}>
          {simpleClassName}
        </a>
      </td>
      <td>
        {klass.statements.headOption.map(_.source.split(File.separatorChar).last).getOrElse("")}
      </td>
      <td>
        {klass.loc.toString}
      </td>
      <td>
        {klass.methodCount.toString}
      </td>
      <td>
        {klass.statementCount.toString}
      </td>
      <td>
        {klass.invokedStatementCount.toString}
      </td>
      <td>
        <div class="meter">
          <span style={s"width: $statement0f%"}></span>
        </div>
      </td>
      <td>
        {klass.statementCoverageFormatted}
        %
      </td>
      <td>
        {klass.branchCount.toString}
      </td>
      <td>
        {klass.invokedBranchesCount.toString}
      </td>
      <td>
        <div class="meter">
          <span style={s"width: $branch0f%"}></span>
        </div>
      </td>
      <td>
        {klass.branchCoverageFormatted}
        %
      </td>
    </tr>
  }

  def packageList(coverage: Coverage): Node = {
    <html>
      <head>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
        <title id='title'>
          Scoverage Code Coverage
        </title>
        {plugins}
      </head>
      <body style="font-family: monospace;">
        <table class="tablesorter table table-striped" style="font-size: 13px">
          <thead>
            <tr>
              <td>
                <a href="overview.html" target="mainFrame">All packages</a>
              </td>
              <td>{coverage.statementCoverageFormatted}%</td>
            </tr>
          </thead>
          <tbody>
            {coverage.packages.map(arg =>
            <tr>
              <td>
                <a href={packageOverviewRelativePath(arg)} target="mainFrame">{arg.name}</a>
              </td>
              <td>{arg.statementCoverageFormatted}%</td>
            </tr>
          )}
          </tbody>
        </table>
      </body>
    </html>
  }

  def risks(coverage: Coverage, limit: Int) = {
    <table class="tablesorter table table-striped" style="font-size: 12px">
      <thead>
        <tr>
          <th>
            Class
          </th>
          <th>
            Lines
          </th>
          <th>
            Methods
          </th>
          <th>
            Statements
          </th>
          <th>
            Statement Rate
          </th>
          <th>
            Branches
          </th>
          <th>
            Branch Rate
          </th>
        </tr>
      </thead>
      <tbody>
        {coverage.risks(limit).map(klass =>
        <tr>
          <td>
            {klass.simpleName}
          </td>
          <td>
            {klass.loc.toString}
          </td>
          <td>
            {klass.methodCount.toString}
          </td>
          <td>
            {klass.statementCount.toString}
          </td>
          <td>
            {klass.statementCoverageFormatted}
            %
          </td>
          <td>
            {klass.branchCount.toString}
          </td>
          <td>
            {klass.branchCoverageFormatted}
            %
          </td>
        </tr>)}
      </tbody>
    </table>
  }

  def packages2(coverage: Coverage) = {
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
          {arg.invokedStatements.toString()}
          /
          {arg.statementCount}
          (
          {arg.statementCoverageFormatted}
          %)
        </td>
      </tr>
    })
    <table>
      {rows}
    </table>
  }

  def overview(coverage: Coverage): Node = {
    <html>
      {header}<body style="font-family: monospace;">
      <div class="alert alert-info">
        <b>
          SCoverage
        </b>
        generated at
        {new Date().toString}
      </div>
      <div class="overview">
        <div class="stats">
          {stats(coverage)}
        </div>
        <div>
          {classesTable(coverage.classes, addPath = true)}
        </div>
      </div>
    </body>
    </html>
  }

  def stats(coverage: Coverage): Node = {

    val statement0f = Math.round(coverage.statementCoveragePercent).toInt.toString
    val branch0f = Math.round(coverage.branchCoveragePercent).toInt.toString


    <table class="table">
      <tr>
        <td>
          Lines of code:
        </td>
        <td>
          {coverage.loc.toString}
        </td>
        <td>
          Files:
        </td>
        <td>
          {coverage.fileCount.toString}
        </td>
        <td>
          Classes:
        </td>
        <td>
          {coverage.classCount.toString}
        </td>
        <td>
          Methods:
        </td>
        <td>
          {coverage.methodCount.toString}
        </td>
      </tr>
      <tr>
        <td>
          Lines per file:
        </td>
        <td>
          {coverage.linesPerFileFormatted}
        </td>
        <td>
          Packages:
        </td>
        <td>
          {coverage.packageCount.toString}
        </td>
        <td>
          Classes per package:
        </td>
        <td>
          {coverage.avgClassesPerPackageFormatted}
        </td>
        <td>
          Methods per class:
        </td>
        <td>
          {coverage.avgMethodsPerClassFormatted}
        </td>
      </tr>
      <tr>
        <td>
          Total statements:
        </td>
        <td>
          {coverage.statementCount.toString}
        </td>
        <td>
          Invoked statements:
        </td>
        <td>
          {coverage.invokedStatementCount.toString}
        </td>
        <td>
          Total branches:
        </td>
        <td>
          {coverage.branchCount.toString}
        </td>
        <td>
          Invoked branches:
        </td>
        <td>
          {coverage.invokedBranchesCount.toString}
        </td>
      </tr>
      <tr>
        <td>
          Ignored statements:
        </td>
        <td>
          {coverage.ignoredStatementCount.toString}
        </td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
      </tr>
      <tr>
        <td>
          Statement coverage:
        </td>
        <td>
          {coverage.statementCoverageFormatted}
          %
        </td>
        <td colspan="2">
          <div class="meter">
            <span style={s"width: $statement0f%"}></span>
          </div>
        </td>
        <td>
          Branch coverage:
        </td>
        <td>
          {coverage.branchCoverageFormatted}
          %
        </td>
        <td colspan="2">
          <div class="meter">
            <span style={s"width: $branch0f%"}></span>
          </div>
        </td>
      </tr>
    </table>
  }
  
  def plugins = {
      <script src="http://ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js"></script>
      <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/jquery.tablesorter/2.20.1/css/theme.default.min.css" type="text/css"/>
      <script src="http://cdnjs.cloudflare.com/ajax/libs/jquery.tablesorter/2.20.1/js/jquery.tablesorter.min.js"></script>
      <link rel="stylesheet" href="http://netdna.bootstrapcdn.com/bootstrap/3.0.3/css/bootstrap.min.css" type="text/css"/>
      <script src="http://netdna.bootstrapcdn.com/bootstrap/3.0.3/js/bootstrap.min.js"></script>
      <script type="text/javascript">
        {xml.Unparsed("""$(document).ready(function() {$(".tablesorter").tablesorter();});""")}
      </script>
  }

}
