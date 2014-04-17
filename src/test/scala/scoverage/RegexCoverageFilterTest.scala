package scoverage

import org.scalatest.FlatSpec
import scala.reflect.internal.util.{NoFile, BatchSourceFile, SourceFile}

class RegexCoverageFilterTest extends FlatSpec {

  "isClassIncluded" should "return true for empty excludes" in {
    assert(new RegexCoverageFilter(Nil).isClassIncluded("x"))
  }

  "isClassIncluded" should "not crash for empty input" in {
    assert(new RegexCoverageFilter(Nil).isClassIncluded(""))
  }

  "isClassIncluded" should "exclude scoverage -> scoverage" in {
    assert(!new RegexCoverageFilter(Seq("scoverage")).isClassIncluded("scoverage"))
  }

  "isClassIncluded" should "include scoverage -> scoverageeee" in {
    assert(new RegexCoverageFilter(Seq("scoverage")).isClassIncluded("scoverageeee"))
  }

  "isClassIncluded" should "exclude scoverage* -> scoverageeee" in {
    assert(!new RegexCoverageFilter(Seq("scoverage*")).isClassIncluded("scoverageeee"))
  }

  "isClassIncluded" should "include eee -> scoverageeee" in {
    assert(new RegexCoverageFilter(Seq("eee")).isClassIncluded("scoverageeee"))
  }

  "isClassIncluded" should "exclude .*eee -> scoverageeee" in {
    assert(!new RegexCoverageFilter(Seq(".*eee")).isClassIncluded("scoverageeee"))
  }

  "getExcludedLineNumbers" should "exclude no lines if no magic comments are found" in {
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

    val numbers = new RegexCoverageFilter(Nil).getExcludedLineNumbers(mockSourceFile(file))
    numbers === List.empty
  }

  "getExcludedLineNumbers" should "exclude lines between magic comments" in {
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

    val numbers = new RegexCoverageFilter(Nil).getExcludedLineNumbers(mockSourceFile(file))
    numbers === List(Range(4,9), Range(12,14))
  }

  "getExcludedLineNumbers" should "exclude all lines after an upaired magic comment" in {
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

    val numbers = new RegexCoverageFilter(Nil).getExcludedLineNumbers(mockSourceFile(file))
    numbers === List(Range(4,9), Range(12,16))
  }

  "getExcludedLineNumbers" should "allow text comments on the same line as the markers" in {
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

    val numbers = new RegexCoverageFilter(Nil).getExcludedLineNumbers(mockSourceFile(file))
    numbers === List(Range(4,9), Range(12,16))
  }

  private def mockSourceFile(contents: String): SourceFile = {
    new BatchSourceFile(NoFile, contents.toCharArray)
  }
}
