package reaktor.scct

import org.specs2.mutable._

class HelloWorldTest extends SpecificationWithJUnit {
  "Hello World" should {
    "concatenate Strings" in {
      HelloWorld.concat(Array("foo","bar")) mustEqual "foobar"
    }
  }
}