package scoverage

object Constants {
  // the final scoverage report
  val XMLReportFilename = "scoverage.xml"
  val XMLReportFilenameWithDebug = "scoverage-debug.xml"
  // directory that contains all the measurement data but not reports
  val DataDir = "scoverage-data"
  /*********************************************************************
   * If updating any of the file names below, an update is also required to *
   * the corresponding file name in Invoker.scala                      *
   *********************************************************************/
  // the file that contains the statement mappings
  val CoverageFileName = "scoverage.coverage"
  // the prefix the measurement files have
  val MeasurementsPrefix = "scoverage.measurements."
  //subdir to suffix to classpath when [writeToClasspath] option is in use"
  val ClasspathSubdir = "META-INF/scoverage"
}
