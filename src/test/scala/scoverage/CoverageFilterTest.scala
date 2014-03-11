package scoverage

import org.scalatest.FlatSpec

class CoverageFilterTest extends FlatSpec {

  "isIncluded" should "return true for empty excludes" in {
    assert(new CoverageFilter(Nil).isIncluded("x"))
  }

  "isIncluded" should "not crash for empty input" in {
    assert(new CoverageFilter(Nil).isIncluded(""))
  }

  "isIncluded" should "exclude scoverage -> scoverage" in {
    assert(!new CoverageFilter(Seq("scoverage")).isIncluded("scoverage"))
  }

  "isIncluded" should "include scoverage -> scoverageeee" in {
    assert(new CoverageFilter(Seq("scoverage")).isIncluded("scoverageeee"))
  }

  "isIncluded" should "exclude scoverage* -> scoverageeee" in {
    assert(!new CoverageFilter(Seq("scoverage*")).isIncluded("scoverageeee"))
  }

  "isIncluded" should "include eee -> scoverageeee" in {
    assert(new CoverageFilter(Seq("eee")).isIncluded("scoverageeee"))
  }

  "isIncluded" should "exclude .*eee -> scoverageeee" in {
    assert(!new CoverageFilter(Seq(".*eee")).isIncluded("scoverageeee"))
  }
}
