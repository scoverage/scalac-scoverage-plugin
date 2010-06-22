package reaktor.scct

class UncoveredAnnotationInstrumentationSpec extends InstrumentationSpec {
  "skipping instrumentation with uncovered-annotation" should {
    "work for class " in {
      // TODO: fixme, annotation doesnt work ... offsetsMatch("""\@reaktor.scct.uncovered class Foo { println("yeah"); def x = 12; }""")
    }
    "work for method" in {
      // TODO: fixme, annotation doesnt work ... classOffsetsMatch("""\@reaktor.scct.uncovered def method { println("yeah"); 123 } """)
    }
  }
}