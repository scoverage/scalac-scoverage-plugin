package reaktor.scct.report

import org.specs.Specification
import reaktor.scct.{ClassTypes, Name, CoveredBlock}

class CoverageDataSpec extends Specification {
  "Percentage calculation" should {
    "calculate total" in {
      new CoverageData(blocks()).percentage mustEqual None
      new CoverageData(blocks(true)).percentage mustEqual Some(100)
      new CoverageData(blocks(false)).percentage mustEqual Some(0)
      new CoverageData(blocks(true, true, false, true)).percentage mustEqual Some(75)
    }
    "skip placeholders" in {
      val data = new CoverageData(CoveredBlock("x", name("x"), 1, true) :: blocks(true, true, false, false))
      data.percentage mustEqual Some(50)
    }
  }

  private def blocks(hits: Boolean*) = {
    1.to(hits.size).map { i =>
      val b = new CoveredBlock(i.toString, name(i.toString), i, false)
      if (hits(i-1)) b.increment else b
    }.toList
  }
  private def name(s: String) = Name(s, ClassTypes.Class, s, s)

}