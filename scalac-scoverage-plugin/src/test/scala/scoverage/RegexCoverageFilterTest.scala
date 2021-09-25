package scoverage

import scala.reflect.internal.util.BatchSourceFile
import scala.reflect.internal.util.NoFile
import scala.reflect.internal.util.SourceFile
import scala.reflect.io.VirtualFile

import munit.FunSuite

class RegexCoverageFilterTest extends FunSuite {

  test("isClassIncluded should return true for empty excludes") {
    assert(new RegexCoverageFilter(Nil, Nil, Nil).isClassIncluded("x"))
  }

  test("should not crash for empty input") {
    assert(new RegexCoverageFilter(Nil, Nil, Nil).isClassIncluded(""))
  }

  test("should exclude scoverage -> scoverage") {
    assert(
      !new RegexCoverageFilter(Seq("scoverage"), Nil, Nil)
        .isClassIncluded("scoverage")
    )
  }

  test("should include scoverage -> scoverageeee") {
    assert(
      new RegexCoverageFilter(Seq("scoverage"), Nil, Nil)
        .isClassIncluded("scoverageeee")
    )
  }

  test("should exclude scoverage* -> scoverageeee") {
    assert(
      !new RegexCoverageFilter(Seq("scoverage*"), Nil, Nil)
        .isClassIncluded("scoverageeee")
    )
  }

  test("should include eee -> scoverageeee") {
    assert(
      new RegexCoverageFilter(Seq("eee"), Nil, Nil)
        .isClassIncluded("scoverageeee")
    )
  }

  test("should exclude .*eee -> scoverageeee") {
    assert(
      !new RegexCoverageFilter(Seq(".*eee"), Nil, Nil)
        .isClassIncluded("scoverageeee")
    )
  }

  val abstractFile = new VirtualFile("sammy.scala")

  test("isFileIncluded should return true for empty excludes") {
    val file = new BatchSourceFile(abstractFile, Array.emptyCharArray)
    assert(new RegexCoverageFilter(Nil, Nil, Nil).isFileIncluded(file))
  }

  test("should exclude by filename") {
    val file = new BatchSourceFile(abstractFile, Array.emptyCharArray)
    assert(
      !new RegexCoverageFilter(Nil, Seq("sammy"), Nil)
        .isFileIncluded(file)
    )
  }

  test("should exclude by regex wildcard") {
    val file = new BatchSourceFile(abstractFile, Array.emptyCharArray)
    assert(
      !new RegexCoverageFilter(Nil, Seq("sam.*"), Nil)
        .isFileIncluded(file)
    )
  }

  test("should not exclude non matching regex") {
    val file = new BatchSourceFile(abstractFile, Array.emptyCharArray)
    assert(
      new RegexCoverageFilter(Nil, Seq("qweqeqwe"), Nil)
        .isFileIncluded(file)
    )
  }

  val options = new ScoverageOptions()

  test("isSymbolIncluded should return true for empty excludes") {
    assert(new RegexCoverageFilter(Nil, Nil, Nil).isSymbolIncluded("x"))
  }

  test("should not crash for empty input") {
    assert(new RegexCoverageFilter(Nil, Nil, Nil).isSymbolIncluded(""))
  }

  test("should exclude scoverage -> scoverage") {
    assert(
      !new RegexCoverageFilter(Nil, Nil, Seq("scoverage"))
        .isSymbolIncluded("scoverage")
    )
  }

  test("should include scoverage -> scoverageeee") {
    assert(
      new RegexCoverageFilter(Nil, Nil, Seq("scoverage"))
        .isSymbolIncluded("scoverageeee")
    )
  }
  test("should exclude scoverage* -> scoverageeee") {
    assert(
      !new RegexCoverageFilter(Nil, Nil, Seq("scoverage*"))
        .isSymbolIncluded("scoverageeee")
    )
  }

  test("should include eee -> scoverageeee") {
    assert(
      new RegexCoverageFilter(Nil, Nil, Seq("eee"))
        .isSymbolIncluded("scoverageeee")
    )
  }

  test("should exclude .*eee -> scoverageeee") {
    assert(
      !new RegexCoverageFilter(Nil, Nil, Seq(".*eee"))
        .isSymbolIncluded("scoverageeee")
    )
  }
  test("should exclude scala.reflect.api.Exprs.Expr") {
    assert(
      !new RegexCoverageFilter(Nil, Nil, options.excludedSymbols)
        .isSymbolIncluded("scala.reflect.api.Exprs.Expr")
    )
  }
  test("should exclude scala.reflect.macros.Universe.Tree") {
    assert(
      !new RegexCoverageFilter(Nil, Nil, options.excludedSymbols)
        .isSymbolIncluded("scala.reflect.macros.Universe.Tree")
    )
  }
  test("should exclude scala.reflect.api.Trees.Tree") {
    assert(
      !new RegexCoverageFilter(Nil, Nil, options.excludedSymbols)
        .isSymbolIncluded("scala.reflect.api.Trees.Tree")
    )
  }
  test(
    "getExcludedLineNumbers should exclude no lines if no magic comments are found"
  ) {
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

    val numbers = new RegexCoverageFilter(Nil, Nil, Nil)
      .getExcludedLineNumbers(mockSourceFile(file))
    assertEquals(numbers, List.empty)
  }
  test("should exclude lines between magic comments") {
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

    val numbers = new RegexCoverageFilter(Nil, Nil, Nil)
      .getExcludedLineNumbers(mockSourceFile(file))
    assertEquals(numbers, List(Range(4, 9), Range(12, 14)))
  }
  test("should exclude all lines after an upaired magic comment") {
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

    val numbers = new RegexCoverageFilter(Nil, Nil, Nil)
      .getExcludedLineNumbers(mockSourceFile(file))
    assertEquals(numbers, List(Range(4, 9), Range(12, 16)))
  }
  test("should allow text comments on the same line as the markers") {
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

    val numbers = new RegexCoverageFilter(Nil, Nil, Nil)
      .getExcludedLineNumbers(mockSourceFile(file))
    assertEquals(numbers, List(Range(4, 9), Range(12, 16)))
  }

  private def mockSourceFile(contents: String): SourceFile = {
    new BatchSourceFile(NoFile, contents.toCharArray)
  }
}
