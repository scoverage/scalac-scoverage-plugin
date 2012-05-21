package reaktor.scct

import org.specs2.mutable._

class AppTest extends SpecificationWithJUnit {
  "App" should {
    "concatenate Strings" in {
      App.foo(Array("foo","bar")) mustEqual "foobar"
    }
  }
}