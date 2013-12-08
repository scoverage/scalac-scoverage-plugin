package scoverage

import java.io.File

/** @author Stephen Samuel */
object Env {
  def coverageFile(dir: File): File = coverageFile(dir.getAbsolutePath)
  def coverageFile(dir: String): File = {
    new File(dir + "/" + Option(System.getenv("scoverage.coverage.file")).getOrElse("scoverage.coverage"))
  }
  def measurementFile(dir: File): File = measurementFile(dir.getAbsolutePath)
  def measurementFile(dir: String): File = {
    new File(dir + "/" + Option(System.getenv("scoverage.measurement.file")).getOrElse("scoverage.measurement"))
  }
}
