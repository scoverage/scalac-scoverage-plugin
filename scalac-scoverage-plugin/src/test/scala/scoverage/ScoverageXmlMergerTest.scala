package scoverage

import org.scalatest.{FreeSpec, Matchers}
import scoverage.report.ScoverageXmlMerger

/** @author Stephen Samuel */
class ScoverageXmlMergerTest extends FreeSpec with Matchers {

  val node1 = scala.xml.XML.load(getClass.getResourceAsStream("/scoverage/report1.xml"))
  val node2 = scala.xml.XML.load(getClass.getResourceAsStream("/scoverage/report2.xml"))

  private def formattedLocally(decimal: BigDecimal) = "%.2f".format(decimal)

  "scoverage xml merger" - {
    "should add top level statement-count" in {
      val node = ScoverageXmlMerger.merge(List(node1, node2))
      (node \ "@statement-count").text.toInt shouldBe 12
    }
    "should add top level statements-invoked" in {
      val node = ScoverageXmlMerger.merge(List(node1, node2))
      (node \ "@statements-invoked").text.toInt shouldBe 11
    }
    "should recalculate statement-rate" in {
      val node = ScoverageXmlMerger.merge(List(node1, node2))
      (node \ "@statement-rate").text shouldBe formattedLocally(91.67)
    }
    "should reset timestamp" in {
      val node = ScoverageXmlMerger.merge(List(node1, node2))
      val original = (node \ "@timestamp").text.toLong
      (node \ "@timestamp").text.toLong shouldBe >=(original)
    }
    "should concatenate all package elements" in {
      val expected = (node1 \\ "package").size + (node2 \\ "package").size
      val node = ScoverageXmlMerger.merge(List(node1, node2))
      (node \\ "package").size shouldBe expected
    }
  }
}
