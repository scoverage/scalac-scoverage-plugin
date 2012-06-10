package reaktor.scct.report

import org.specs.Specification
import reaktor.scct.CoveredBlock

class CoverageDataSpec extends Specification {
  import reaktor.scct.CoveredBlockGenerator._

  "Percentage calculation" should {
    "calculate total" in {
      new CoverageData(blocks()).percentage mustEqual None
      new CoverageData(blocks(true)).percentage mustEqual Some(100)
      new CoverageData(blocks(false)).percentage mustEqual Some(0)
      new CoverageData(blocks(true, true, false, true)).percentage mustEqual Some(75)
      new CoverageData(blocks(true, true, false)).percentage mustEqual Some(66)
    }
    "skip placeholders" in {
      val data = new CoverageData(CoveredBlock("x", blockName("x"), 1, true) :: blocks(true, true, false, false))
      data.percentage mustEqual Some(50)
    }
  }

  "Rate calculation" should {
    "calculate total" in {
      new CoverageData(blocks()).rate mustEqual None
      new CoverageData(blocks(true)).rate mustEqual Some(1)
      new CoverageData(blocks(false)).rate mustEqual Some(0)
      new CoverageData(blocks(true, true, false, true)).rate mustEqual Some(0.75)
      new CoverageData(blocks(true, true, false)).rate mustEqual Some(0.66)
    }
    "skip placeholders" in {
      val data = new CoverageData(CoveredBlock("x", blockName("x"), 1, true) :: blocks(true, true, false, false))
      data.rate mustEqual Some(0.5)
    }
  }
}