package scoverage.report

import java.io.File

import scoverage.{Coverage, IOUtils}

object CoverageAggregator {

  def aggregate(baseDir: File): Option[Coverage] = {
    val files = IOUtils.reportFileSearch(baseDir, IOUtils.isReportFile)
    println(s"[info] Found ${files.size} subproject report files [${files.mkString(",")}]")
    if (files.size > 0) {
      val coverage = aggregatedCoverage(files)
      Some(coverage)
    } else {
      None
    }
  }

   def aggregatedCoverage(files: Seq[File]): Coverage = {
    var id = 0
    val coverage = Coverage()
    files foreach {
      case file =>
        val subcoverage = ScoverageXmlReader.read(file)
        // need to ensure all the ids are unique otherwise the coverage object will have stmt collisions
        id = id + 1
        subcoverage.statements foreach { stmt => coverage add stmt.copy(id = id)}
    }
    coverage
  }
}
