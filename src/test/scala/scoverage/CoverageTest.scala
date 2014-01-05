package scoverage

import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest

/** @author Stephen Samuel */
class CoverageTest extends FunSuite with BeforeAndAfter with OneInstancePerTest {

  test("coverage for no statements is 1") {
    val coverage = Coverage()
    assert(1.0 === coverage.statementCoverage)
  }

  test("coverage for no invoked statements is 0") {
    val coverage = Coverage()
    coverage.add(MeasuredStatement("", Location("", "", ClassType.Object, ""), 1, 2, 3, 4, "", "", "", false, 0))
    assert(0 === coverage.statementCoverage)
  }

  test("coverage for invoked statements") {
    val coverage = Coverage()
    coverage.add(MeasuredStatement("", Location("", "", ClassType.Object, ""), 1, 2, 3, 4, "", "", "", false, 3))
    coverage.add(MeasuredStatement("", Location("", "", ClassType.Object, ""), 1, 2, 3, 4, "", "", "", false, 0))
    coverage.add(MeasuredStatement("", Location("", "", ClassType.Object, ""), 1, 2, 3, 4, "", "", "", false, 0))
    coverage.add(MeasuredStatement("", Location("", "", ClassType.Object, ""), 1, 2, 3, 4, "", "", "", false, 0))
    assert(0.25 === coverage.statementCoverage)
  }
}
