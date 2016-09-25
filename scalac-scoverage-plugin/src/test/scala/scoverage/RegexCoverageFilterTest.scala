package scoverage

import org.mockito.Mockito
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FreeSpec, Matchers}

import scala.reflect.internal.util._
import scala.reflect.io.AbstractFile

class RegexCoverageFilterTest extends FreeSpec with Matchers with MockitoSugar {

  "isClassIncluded" - {

    "should return true for empty excludes" in {
      assert(new RegexCoverageFilter(Nil, Nil, Nil).isClassIncluded("x"))
    }

    "should not crash for empty input" in {
      assert(new RegexCoverageFilter(Nil, Nil, Nil).isClassIncluded(""))
    }

    "should exclude scoverage -> scoverage" in {
      assert(!new RegexCoverageFilter(Seq("scoverage"), Nil, Nil).isClassIncluded("scoverage"))
    }

    "should include scoverage -> scoverageeee" in {
      assert(new RegexCoverageFilter(Seq("scoverage"), Nil, Nil).isClassIncluded("scoverageeee"))
    }

    "should exclude scoverage* -> scoverageeee" in {
      assert(!new RegexCoverageFilter(Seq("scoverage*"), Nil, Nil).isClassIncluded("scoverageeee"))
    }

    "should include eee -> scoverageeee" in {
      assert(new RegexCoverageFilter(Seq("eee"), Nil, Nil).isClassIncluded("scoverageeee"))
    }

    "should exclude .*eee -> scoverageeee" in {
      assert(!new RegexCoverageFilter(Seq(".*eee"), Nil, Nil).isClassIncluded("scoverageeee"))
    }
  }
  "isFileIncluded" - {
    val abstractFile = mock[AbstractFile]
    Mockito.when(abstractFile.path).thenReturn("sammy.scala")
    "should return true for empty excludes" in {
      val file = new BatchSourceFile(abstractFile, Array.emptyCharArray)
      new RegexCoverageFilter(Nil, Nil, Nil).isFileIncluded(file) shouldBe true
    }
    "should exclude by filename" in {
      val file = new BatchSourceFile(abstractFile, Array.emptyCharArray)
      new RegexCoverageFilter(Nil, Seq("sammy"), Nil).isFileIncluded(file) shouldBe false
    }
    "should exclude by regex wildcard" in {
      val file = new BatchSourceFile(abstractFile, Array.emptyCharArray)
      new RegexCoverageFilter(Nil, Seq("sam.*"), Nil).isFileIncluded(file) shouldBe false
    }
    "should not exclude non matching regex" in {
      val file = new BatchSourceFile(abstractFile, Array.emptyCharArray)
      new RegexCoverageFilter(Nil, Seq("qweqeqwe"), Nil).isFileIncluded(file) shouldBe true
    }
  }
  "isSymbolIncluded" - {
    val options = new ScoverageOptions()
    "should return true for empty excludes" in {
      assert(new RegexCoverageFilter(Nil, Nil, Nil).isSymbolIncluded("x"))
    }

    "should not crash for empty input" in {
      assert(new RegexCoverageFilter(Nil, Nil, Nil).isSymbolIncluded(""))
    }

    "should exclude scoverage -> scoverage" in {
      assert(!new RegexCoverageFilter(Nil, Nil, Seq("scoverage")).isSymbolIncluded("scoverage"))
    }

    "should include scoverage -> scoverageeee" in {
      assert(new RegexCoverageFilter(Nil, Nil, Seq("scoverage")).isSymbolIncluded("scoverageeee"))
    }
    "should exclude scoverage* -> scoverageeee" in {
      assert(!new RegexCoverageFilter(Nil, Nil, Seq("scoverage*")).isSymbolIncluded("scoverageeee"))
    }

    "should include eee -> scoverageeee" in {
      assert(new RegexCoverageFilter(Nil, Nil, Seq("eee")).isSymbolIncluded("scoverageeee"))
    }

    "should exclude .*eee -> scoverageeee" in {
      assert(!new RegexCoverageFilter(Nil, Nil, Seq(".*eee")).isSymbolIncluded("scoverageeee"))
    }
    "should exclude scala.reflect.api.Exprs.Expr" in {
      assert(!new RegexCoverageFilter(Nil, Nil, options.excludedSymbols).isSymbolIncluded("scala.reflect.api.Exprs.Expr"))
    }
    "should exclude scala.reflect.macros.Universe.Tree" in {
      assert(!new RegexCoverageFilter(Nil, Nil, options.excludedSymbols).isSymbolIncluded("scala.reflect.macros.Universe.Tree"))
    }
    "should exclude scala.reflect.api.Trees.Tree" in {
      assert(!new RegexCoverageFilter(Nil, Nil, options.excludedSymbols).isSymbolIncluded("scala.reflect.api.Trees.Tree"))
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

      val numbers = new RegexCoverageFilter(Nil, Nil, Nil).getExcludedLineNumbers(mockSourceFile(file))
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

      val numbers = new RegexCoverageFilter(Nil, Nil, Nil).getExcludedLineNumbers(mockSourceFile(file))
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

      val numbers = new RegexCoverageFilter(Nil, Nil, Nil).getExcludedLineNumbers(mockSourceFile(file))
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

      val numbers = new RegexCoverageFilter(Nil, Nil, Nil).getExcludedLineNumbers(mockSourceFile(file))
      numbers === List(Range(4, 9), Range(12, 16))
    }
  }

  private def mockSourceFile(contents: String): SourceFile = {
    new BatchSourceFile(NoFile, contents.toCharArray)
  }
}
