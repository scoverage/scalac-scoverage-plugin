package scales

import java.util.concurrent.atomic.AtomicInteger
import scala.reflect.internal.util.SourceFile

/** @author Stephen Samuel */
object InstrumentationRuntime {

  val ids = new AtomicInteger(0)
  val coverage = new Coverage

  /**
   * Registers a new MeasuredStatement that will be potentially invoked during the test phase.
   * Each MeasuredStatement has a unique id which is used when calling invoked(id).
   */
  def add(source: SourceFile,
          location: Location,
          start: Int,
          line: Int,
          desc: String) = {
    val id = ids.incrementAndGet()
    val stmt = MeasuredStatement(source, location, id, start, line, desc)
    coverage.add(stmt)
    stmt
  }

  /**
   * Sets the MeasuredStatement wtih the given id to invoked.
   * Idempotent operation.
   */
  def invoked(id: Int): Int = {
    coverage.invoked(id)
    id
  }
}



