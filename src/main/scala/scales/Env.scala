package scales

import java.io.File

/** @author Stephen Samuel */
object Env {
  def measurementFile = new File("scales.measurement")
  def coverageFile = new File("scales.coverage")
}
