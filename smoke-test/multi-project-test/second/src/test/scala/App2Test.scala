import org.specs2.mutable._
import reaktor.scct.App2

class App2Test extends SpecificationWithJUnit {
  "App2" should {
    "have a main" in {
      App2.main(Array("foo","bar")) mustEqual 0
    }
  }
}