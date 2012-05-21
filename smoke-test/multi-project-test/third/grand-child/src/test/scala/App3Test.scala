import org.specs2.mutable._
import reaktor.scct.App3

class App3Test extends SpecificationWithJUnit {
  "App3" should {
    "have a main" in {
      App3.main(Array("foo","bar")) mustEqual 0
    }
  }
}