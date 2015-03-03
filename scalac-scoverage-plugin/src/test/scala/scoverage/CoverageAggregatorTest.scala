package scoverage

import java.io.File
import java.util.UUID

import org.scalatest.{FreeSpec, Matchers}
import scoverage.report.{CoverageAggregator, ScoverageXmlWriter}

class CoverageAggregatorTest extends FreeSpec with Matchers {

  // Let current directory be our source root
  private val sourceRoot = new File(".")
  private def canonicalPath(fileName: String) = new File(sourceRoot, fileName).getCanonicalPath

  "coverage aggregator" - {
    "should merge coverage objects with same id" in {

      val source = canonicalPath("com/scoverage/class.scala")
      val location = Location("com.scoverage.foo",
        "ServiceState",
        "Service",
        ClassType.Trait,
        "methlab",
        source)

      val coverage1 = Coverage()
      coverage1.add(Statement(source, location, 1, 155, 176, 4, "", "", "", true, 1))
      coverage1.add(Statement(source, location, 2, 200, 300, 5, "", "", "", false, 2))
      val dir1 = new File(IOUtils.getTempPath, UUID.randomUUID.toString)
      dir1.mkdir()
      new ScoverageXmlWriter(sourceRoot, dir1, false).write(coverage1)

      val coverage2 = Coverage()
      coverage2.add(Statement(source, location, 1, 95, 105, 19, "", "", "", false, 0))
      val dir2 = new File(IOUtils.getTempPath, UUID.randomUUID.toString)
      dir2.mkdir()
      new ScoverageXmlWriter(sourceRoot, dir2, false).write(coverage2)

      val coverage3 = Coverage()
      coverage3.add(Statement(source, location, 2, 14, 1515, 544, "", "", "", false, 1))
      val dir3 = new File(IOUtils.getTempPath, UUID.randomUUID.toString)
      dir3.mkdir()
      new ScoverageXmlWriter(sourceRoot, dir3, false).write(coverage3)

      val aggregated = CoverageAggregator.aggregatedCoverage(
        Seq(IOUtils.reportFile(dir1, debug = false),
          IOUtils.reportFile(dir2, debug = false),
          IOUtils.reportFile(dir3, debug = false))
      )
      aggregated.statements.toSet.size shouldBe 4
      aggregated.statements.map(_.copy(id = 0)).toSet shouldBe
        (coverage1.statements ++ coverage2.statements ++ coverage3.statements).map(_.copy(id = 0)).toSet
    }
  }
}
