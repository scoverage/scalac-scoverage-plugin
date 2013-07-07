package scales.report

import scales.Coverage

/** @author Stephen Samuel */
trait ScalesWriter {

    def write(coverage: Coverage)
}
