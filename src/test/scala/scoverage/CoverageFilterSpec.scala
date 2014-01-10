package scoverage

import org.specs2.mutable.Specification

class CoverageFilterSpec extends Specification {
  "isIncluded" should {
    "return true for empty excludes" in {
      new CoverageFilter(Nil).isIncluded("x") must beTrue
    }

    "not crash for empty input" in {
      new CoverageFilter(Nil).isIncluded("") must beTrue
    }

    "exclude scoverage -> scoverage" in {
      new CoverageFilter(Seq("scoverage")).isIncluded("scoverage") must beFalse
    }

    "include scoverage -> scoverageeee" in {
      new CoverageFilter(Seq("scoverage")).isIncluded("scoverageeee") must beTrue
    }

    "exclude scoverage* -> scoverageeee" in {
      new CoverageFilter(Seq("scoverage*")).isIncluded("scoverageeee") must beFalse
    }

    "include eee -> scoverageeee" in {
      new CoverageFilter(Seq("eee")).isIncluded("scoverageeee") must beTrue
    }

    "exclude .*eee -> scoverageeee" in {
      new CoverageFilter(Seq(".*eee")).isIncluded("scoverageeee") must beFalse
    }
  }
}
