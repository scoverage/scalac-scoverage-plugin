package scoverage.report

import java.io.File

import scoverage.Coverage
import scoverage.IOUtils
import scoverage.Serializer

object CoverageAggregator {

  @deprecated("1.4.0", "Used only by gradle-scoverage plugin")
  def aggregate(baseDir: File, clean: Boolean): Option[Coverage] = {
    aggregate(IOUtils.scoverageDataDirsSearch(baseDir))
  }

  // to be used by gradle-scoverage plugin
  def aggregate(dataDirs: Array[File]): Option[Coverage] = aggregate(
    dataDirs.toSeq
  )

  def aggregate(dataDirs: Seq[File]): Option[Coverage] = {
    println(
      s"[info] Found ${dataDirs.size} subproject scoverage data directories [${dataDirs.mkString(",")}]"
    )
    if (dataDirs.size > 0) {
      Some(aggregatedCoverage(dataDirs))
    } else {
      None
    }
  }

  def aggregatedCoverage(dataDirs: Seq[File]): Coverage = {
    var id = 0
    val coverage = Coverage()
    dataDirs foreach { dataDir =>
      val coverageFile: File = Serializer.coverageFile(dataDir)
      if (coverageFile.exists) {
        val subcoverage: Coverage = Serializer.deserialize(coverageFile)
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
