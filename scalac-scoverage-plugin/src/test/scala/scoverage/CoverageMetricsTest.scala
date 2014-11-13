package scoverage

import org.scalatest.{FreeSpec, Matchers}
import scoverage.{CoverageMetrics, Statement}

class CoverageMetricsTest extends FreeSpec with Matchers {

  "no branches with at least one invoked statement should have 100% branch coverage" in {
    val metrics = new CoverageMetrics {
      override def statements: Iterable[Statement] = Seq(Statement(null,
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
    }
    metrics.branchCount shouldBe 0
    metrics.branchCoverage - 1 shouldBe < (0.0001)
  }

  "no branches with no invoked statements should have 0% branch coverage" in {
    val metrics = new CoverageMetrics {
      override def statements: Iterable[Statement] = Seq(Statement(null,
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
    }
    metrics.branchCount shouldBe 0
    metrics.branchCoverage shouldBe 0
  }
}
