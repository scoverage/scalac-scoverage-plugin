package scoverage

object Constants {
  // the file that contains the statement mappings
  val CoverageFileName = "scoverage.coverage.xml"
  // the final scoverage report
  val XMLReportFilename = "scoverage.xml"
  val XMLReportFilenameWithDebug = "scoverage-debug.xml"
  // directory that contains all the measurement data but not reports
  val DataDir = "scoverage-data"
  // the prefix the measurement files have
  val MeasurementsPrefix = "scoverage.measurements."
}
