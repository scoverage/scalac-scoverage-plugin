package scoverage

import java.io.File

import scoverage.domain.Coverage

object CoverageMerge {
  def mergePreviousAndCurrentCoverage(
      lastCompiledFiles: Set[String],
      previousCoverage: Coverage,
      currentCoverage: Coverage
  ): Coverage = {
    val mergedCoverage = Coverage()

    previousCoverage.statements
      .filterNot(stmt =>
        lastCompiledFiles.contains(stmt.source) ||
          !new File(stmt.source).exists()
      )
      .foreach { stmt =>
        mergedCoverage.add(stmt)
      }
    currentCoverage.statements.foreach(stmt => mergedCoverage.add(stmt))

    mergedCoverage
  }
}
