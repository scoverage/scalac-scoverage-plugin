package scoverage

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class CoverageMetricsTest extends AnyFreeSpec with Matchers {

  "no branches with at least one invoked statement should have 100% branch coverage" in {
    val metrics = new CoverageMetrics {
      override def statements: Iterable[Statement] = Seq(Statement(
        null,
        0,
        0,
        0,
        0,
        null,
        null,
        null,
        false,
        1))

      override def ignoredStatements: Iterable[Statement] = Seq()
    }
    metrics.branchCount shouldBe 0
    metrics.branchCoverage shouldBe 1.0 +- 0.0001
  }

  "no branches with no invoked statements should have 0% branch coverage" in {
    val metrics = new CoverageMetrics {
      override def statements: Iterable[Statement] = Seq(Statement(
        null,
        0,
        0,
        0,
        0,
        null,
        null,
        null,
        false,
        0))

      override def ignoredStatements: Iterable[Statement] = Seq()
    }
    metrics.branchCount shouldBe 0
    metrics.branchCoverage shouldBe 0
  }
}
