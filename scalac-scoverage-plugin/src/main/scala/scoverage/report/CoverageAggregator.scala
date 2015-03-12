package scoverage.report

import java.io.File

import scoverage.{Coverage, IOUtils}

object CoverageAggregator {

  def aggregate(baseDir: File, clean: Boolean): Option[Coverage] = {
    val files = IOUtils.reportFileSearch(baseDir, IOUtils.isReportFile)
    aggregate(files, clean)
  }

  def aggregate(files: Seq[File], clean: Boolean): Option[Coverage] = {
    println(s"[info] Found ${files.size} subproject report files [${files.mkString(",")}]")
    if (files.size > 1) {
      val coverage = aggregatedCoverage(files)
      if (clean) files foreach (_.delete)
      Some(coverage)
    } else {
      None
    }
  }

  def aggregatedCoverage(files: Seq[File]): Coverage = {
    var id = 0
    val coverage = Coverage()
    files foreach { file =>
      val subcoverage = ScoverageXmlReader.read(file)
      subcoverage.statements foreach { stmt =>
        // need to ensure all the ids are unique otherwise the coverage object will have stmt collisions
        id = id + 1
        coverage add stmt.copy(id = id)
      }
    }
    coverage
  }
}
