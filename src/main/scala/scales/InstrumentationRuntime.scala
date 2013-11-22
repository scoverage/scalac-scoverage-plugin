package scales

import java.util.concurrent.atomic.AtomicInteger

/** @author Stephen Samuel */
object InstrumentationRuntime {

  val ids = new AtomicInteger(0)
  var coverage = new Coverage

  def setCoverage(coverage: Coverage): Unit = this.coverage = coverage

  /**
   * Registers a new MeasuredStatement that will be potentially invoked during the test phase.
   * Each MeasuredStatement has a unique id which is used when calling invoked(id).
   */
  def add(source: String,
          location: Location,
          start: Int,
          line: Int,
          desc: String,
          branch: Boolean) = {
    val id = ids.incrementAndGet()
    val stmt = MeasuredStatement(source, location, id, start, line, desc, branch)
    coverage.add(stmt)
    stmt
  }
}



