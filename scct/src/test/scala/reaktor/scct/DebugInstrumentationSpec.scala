package reaktor.scct

class DebugInstrumentationSpec extends InstrumentationSpec {
  override def debug = true

  "debug instrumentation" in {
    1 mustEqual 1
  }
}