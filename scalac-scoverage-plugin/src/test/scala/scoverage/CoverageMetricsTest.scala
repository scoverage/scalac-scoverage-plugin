package scoverage

import munit.FunSuite

class CoverageMetricsTest extends FunSuite {

  test(
    "no branches with at least one invoked statement should have 100% branch coverage"
  ) {
    val metrics = new CoverageMetrics {
      override def statements: Iterable[Statement] =
        Seq(Statement(null, 0, 0, 0, 0, null, null, null, false, 1))

      override def ignoredStatements: Iterable[Statement] = Seq()
    }
    assertEquals(metrics.branchCount, 0)
    assertEqualsDouble(metrics.branchCoverage, 1.0, 0.0001)
  }

  test(
    "no branches with no invoked statements should have 0% branch coverage"
  ) {
    val metrics = new CoverageMetrics {
      override def statements: Iterable[Statement] =
        Seq(Statement(null, 0, 0, 0, 0, null, null, null, false, 0))

      override def ignoredStatements: Iterable[Statement] = Seq()
    }
    assertEquals(metrics.branchCount, 0)
    assertEquals(metrics.branchCoverage, 0.0)
  }
}
