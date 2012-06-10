package reaktor.scct

class DebugInstrumentationSpec extends InstrumentationSpec {
  //override def debug = true

  "debug instrumentation" in {
    classOffsetsMatch("@Some(\"foo\").map(@System.getProperty)")
  }
}