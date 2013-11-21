package scales.report

import scales.Coverage

/** @author Stephen Samuel */
trait ReportWriter {
  def write(coverage: Coverage): String
}
