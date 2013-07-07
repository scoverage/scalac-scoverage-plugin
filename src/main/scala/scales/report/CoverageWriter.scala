package scales.report

import scales.Coverage

/** @author Stephen Samuel */
trait CoverageWriter {

    def write(coverage: Coverage)
}
