package scoverage.report

import scoverage.Coverage
import java.io.File

/** @author Stephen Samuel */
trait CoverageWriter {
  def write(coverage: Coverage, dir: File)
}
