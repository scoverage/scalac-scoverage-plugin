package scoverage.reporter

import java.io.File

object CoverageAggregator {

  // to be used by gradle-scoverage plugin
  def aggregate(dataDirs: Array[File], sourceRoot: File): Option[Coverage] =
    aggregate(
      dataDirs.toSeq,
      sourceRoot
    )

  def aggregate(dataDirs: Seq[File], sourceRoot: File): Option[Coverage] = {
    println(
      s"[info] Found ${dataDirs.size} subproject scoverage data directories [${dataDirs.mkString(",")}]"
    )
    if (dataDirs.size > 0) {
      Some(aggregatedCoverage(dataDirs, sourceRoot))
    } else {
      None
    }
  }

  def aggregatedCoverage(dataDirs: Seq[File], sourceRoot: File): Coverage = {
    var id = 0
    val coverage = Coverage()
    dataDirs foreach { dataDir =>
      val coverageFile: File = IOUtils.coverageFile(dataDir)
      if (coverageFile.exists) {
        val subcoverage: Coverage =
          Serializer.deserialize(coverageFile, sourceRoot)
        val measurementFiles: Array[File] =
          IOUtils.findMeasurementFiles(dataDir)
        val measurements = IOUtils.invoked(measurementFiles.toIndexedSeq)
        subcoverage.apply(measurements)
        subcoverage.statements foreach { stmt =>
          // need to ensure all the ids are unique otherwise the coverage object will have stmt collisions
          id = id + 1
          coverage add stmt.copy(id = id)
        }
      }
    }
    coverage
  }
}
