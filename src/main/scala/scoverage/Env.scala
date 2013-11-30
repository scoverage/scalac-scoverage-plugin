package scoverage

import java.io.File

/** @author Stephen Samuel */
object Env {
  def measurementFile = {
    new File(Option(System.getenv("scoverage.measurement.file")).getOrElse("target/scoverage.measurement"))
  }
  def coverageFile = {
    new File(Option(System.getenv("scoverage.coverage.file")).getOrElse("target/scoverage.coverage"))
  }
}
