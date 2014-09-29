package scoverage

import org.scalatest.FreeSpec

import scala.reflect.internal.util.{BatchSourceFile, NoFile, SourceFile}

class RegexCoverageFilterTest extends FreeSpec {

  "isClassIncluded" - {

    "should return true for empty excludes" in {
      assert(new RegexCoverageFilter(Nil, Nil).isClassIncluded("x"))
    }

    "should not crash for empty input" in {
      assert(new RegexCoverageFilter(Nil, Nil).isClassIncluded(""))
    }

    "should exclude scoverage -> scoverage" in {
      assert(!new RegexCoverageFilter(Seq("scoverage"), Nil).isClassIncluded("scoverage"))
    }

    "should include scoverage -> scoverageeee" in {
      assert(new RegexCoverageFilter(Seq("scoverage"), Nil).isClassIncluded("scoverageeee"))
    }

    "should exclude scoverage* -> scoverageeee" in {
      assert(!new RegexCoverageFilter(Seq("scoverage*"), Nil).isClassIncluded("scoverageeee"))
    }

    "should include eee -> scoverageeee" in {
      assert(new RegexCoverageFilter(Seq("eee"), Nil).isClassIncluded("scoverageeee"))
    }

    "should exclude .*eee -> scoverageeee" in {
      assert(!new RegexCoverageFilter(Seq(".*eee"), Nil).isClassIncluded("scoverageeee"))
    }
  }
  "getExcludedLineNumbers" - {
    "should exclude no lines if no magic comments are found" in {
      val file =
        """1
          |2
          |3
          |4
          |5
          |6
          |7
          |8
        """.stripMargin

      val numbers = new RegexCoverageFilter(Nil, Nil).getExcludedLineNumbers(mockSourceFile(file))
      numbers === List.empty
    }
    "should exclude lines between magic comments" in {
      val file =
        """1
          |2
          |3
          |  // $COVERAGE-OFF$
          |5
          |6
          |7
          |8
          |    // $COVERAGE-ON$
          |10
          |11
          |    // $COVERAGE-OFF$
          |13
          |    // $COVERAGE-ON$
          |15
          |16
        """.stripMargin

      val numbers = new RegexCoverageFilter(Nil, Nil).getExcludedLineNumbers(mockSourceFile(file))
      numbers === List(Range(4, 9), Range(12, 14))
    }
    "should exclude all lines after an upaired magic comment" in {
      val file =
        """1
          |2
          |3
          |  // $COVERAGE-OFF$
          |5
          |6
          |7
          |8
          |    // $COVERAGE-ON$
          |10
          |11
          |    // $COVERAGE-OFF$
          |13
          |14
          |15
        """.stripMargin

      val numbers = new RegexCoverageFilter(Nil, Nil).getExcludedLineNumbers(mockSourceFile(file))
      numbers === List(Range(4, 9), Range(12, 16))
    }
    "should allow text comments on the same line as the markers" in {
      val file =
        """1
          |2
          |3
          |  // $COVERAGE-OFF$ because the next lines are boring
          |5
          |6
          |7
          |8
          |    // $COVERAGE-ON$ resume coverage here
          |10
          |11
          |    // $COVERAGE-OFF$ but ignore this bit
          |13
          |14
          |15
        """.stripMargin

      val numbers = new RegexCoverageFilter(Nil, Nil).getExcludedLineNumbers(mockSourceFile(file))
      numbers === List(Range(4, 9), Range(12, 16))
    }
  }

  private def mockSourceFile(contents: String): SourceFile = {
    new BatchSourceFile(NoFile, contents.toCharArray)
  }
}
