package reaktor.scct

class LazyValInstrumentationSpec extends InstrumentationSpec {
  "Lazy val instrumentation" should instrument {
    "basic lazy val" in {
      classOffsetsMatch("lazy val foo = @System.currentTimeMillis")
    }
    "lazy val with block" in {
      classOffsetsMatch("""lazy val foo = { @println("1"); @System.currentTimeMillis }""")
    }
    "lazy val in def" in {
      classOffsetsMatch("def myMethod = { lazy val foo = @System.currentTimeMillis; @foo }")
      classOffsetsMatch("def myMethod { lazy val foo = @System.currentTimeMillis; @foo }")
    }
  }
}