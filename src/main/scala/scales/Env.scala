package scales

import java.io.File

/** @author Stephen Samuel */
object Env {
  def measurementFile = new File(Option(System.getProperty("scales.measurement.file")).getOrElse("scales.measurement"))
  def coverageFile = new File(Option(System.getProperty("scales.coverage.file")).getOrElse("scales.coverage"))
}
