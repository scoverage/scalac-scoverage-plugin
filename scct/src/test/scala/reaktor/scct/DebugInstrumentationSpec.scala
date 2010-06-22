package reaktor.scct

class DebugInstrumentationSpec extends InstrumentationSpec {
  "debug" in {
    offsetsMatch("class Foo @{ class Bar @}")
  }
}