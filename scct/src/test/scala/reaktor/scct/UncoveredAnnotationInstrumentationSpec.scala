package reaktor.scct

class UncoveredAnnotationInstrumentationSpec extends InstrumentationSpec {
  "skipping instrumentation with uncovered-annotation" should {
    "work for class " in {
      offsetsMatch("""\@reaktor.scct.uncovered class Foo { println("yeah"); def x = 12; }""")
    }
    "work for method" in {
      classOffsetsMatch("""\@reaktor.scct.uncovered def method { println("yeah"); 123 } """)
    }
  }
}