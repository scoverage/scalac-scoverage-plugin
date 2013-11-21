package scales

import java.io.File

/** @author Stephen Samuel */
object Env {
  def measurementFile = {
    new File(Option(System.getenv("scales.measurement.file")).getOrElse("target/scales.measurement"))
  }
  def coverageFile = {
    new File(Option(System.getenv("scales.coverage.file")).getOrElse("target/scales.coverage"))
  }
}
