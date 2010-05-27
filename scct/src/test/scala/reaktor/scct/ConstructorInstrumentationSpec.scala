package reaktor.scct

class ConstructorInstrumentationSpec extends InstrumentationSpec {
  "Constructor instrumentation" should instrument {
    "basic auxiliary constructors" in {
      offsetsMatch("class @Foo(x: Int) { def this(s: String) = @this(s.toInt) }")
    }
    "complicated auxiliary constructors" in {
      offsetsMatch("class @Foo(s: String) { def this(x: Int) = @this((x + x).toString) }")
    }

    "extending class constructors" in {
      offsetsMatch("class @Foo(x: Int)\nclass @Bar(x: Int, y: Int) extends Foo(x)")
    }
  }
}