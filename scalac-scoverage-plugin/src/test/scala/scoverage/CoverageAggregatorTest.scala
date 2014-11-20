package scoverage

import java.io.File
import java.util.UUID

import org.scalatest.{FreeSpec, Matchers}
import scoverage.report.{ScoverageXmlWriter, CoverageAggregator}

class CoverageAggregatorTest extends FreeSpec with Matchers {

  "coverage aggregator" - {
    "should merge coverage objects" in {

      val coverage1 = Coverage()
      coverage1.add(Statement("/home/sam/src/main/scala/com/scoverage/class.scala",
        Location("com.scoverage",
          "Test",
          "TopLevel",
          ClassType.Object,
          "somemeth",
          "/home/sam/src/main/scala/com/scoverage/class.scala"),
        14,
        155,
        176,
        4,
        "",
        "",
        "",
        true,
        2))

      val dir1 = new File(IOUtils.getTempPath, UUID.randomUUID.toString)
      dir1.mkdir()
      new ScoverageXmlWriter(new File("/home/sam"), dir1, false).write(coverage1)

      val coverage2 = Coverage()
      coverage2.add(Statement("/home/sam/src/main/scala/com/scoverage/foo/class.scala",
        Location("com.scoverage.foo",
          "ServiceState",
          "Service",
          ClassType.Trait,
          "methlab",
          "/home/sam/src/main/scala/com/scoverage/foo/class.scala"),
        16,
        95,
        105,
        19,
        "",
        "",
        "",
        false,
        0))

      val dir2 = new File(IOUtils.getTempPath, UUID.randomUUID.toString)
      dir2.mkdir()
      new ScoverageXmlWriter(new File("/home/sam"), dir2, false).write(coverage2)

      val aggregated = CoverageAggregator.aggregatedCoverage(
        Seq(IOUtils.reportFile(dir1, debug = false), IOUtils.reportFile(dir2, debug = false))
      )
      aggregated.statements.map(_.copy(id = 0)).toSet shouldEqual (coverage1.statements ++ coverage2.statements).map(_.copy(id = 0)).toSet
    }
  }
}
