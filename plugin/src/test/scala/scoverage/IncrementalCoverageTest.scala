package scoverage

import munit.FunSuite

class IncrementalCoverageTest extends FunSuite {

  case class Compilation(basePath: java.io.File, code: String) {
    val coverageFile = serialize.Serializer.coverageFile(basePath)
    val compiler = ScoverageCompiler(basePath = basePath)

    val file = compiler.writeCodeSnippetToTempFile(code)
    compiler.compileSourceFiles(file)
    compiler.assertNoErrors()

    val coverage = serialize.Serializer.deserialize(coverageFile, basePath)
  }

  test(
    "should keep coverage from previous compilation when compiling incrementally"
  ) {
    val basePath = ScoverageCompiler.tempBasePath()

    val compilation1 =
      Compilation(basePath, """object First { def test(): Int = 42 }""")

    locally {
      val sourceFiles = compilation1.coverage.files.map(_.source).toSet
      assertEquals(sourceFiles, Set(compilation1.file.getCanonicalPath))
    }

    val compilation2 =
      Compilation(
        basePath,
        """object Second { def test(): String = "hello" }"""
      )

    locally {
      val sourceFiles = compilation2.coverage.files.map(_.source).toSet
      assertEquals(
        sourceFiles,
        Set(compilation1.file, compilation2.file).map(_.getCanonicalPath)
      )
    }
  }

  test(
    "should not keep coverage from previous compilation if the source file was deleted"
  ) {
    val basePath = ScoverageCompiler.tempBasePath()

    val compilation1 =
      Compilation(basePath, """object First { def test(): Int = 42 }""")

    locally {
      val sourceFiles = compilation1.coverage.files.map(_.source).toSet

      assertEquals(sourceFiles, Set(compilation1.file.getCanonicalPath))
    }

    compilation1.file.delete()

    val compilation2 = Compilation(basePath, "")

    locally {
      val sourceFiles = compilation2.coverage.files.map(_.source).toSet
      assertEquals(sourceFiles, Set.empty[String])
    }
  }

  test(
    "should not keep coverage from previous compilation if the source file was compiled again"
  ) {
    val basePath = ScoverageCompiler.tempBasePath()

    val compilation1 =
      Compilation(basePath, """object First { def test(): Int = 42 }""")

    reporter.IOUtils.writeToFile(
      compilation1.file,
      """object Second { def test(): String = "hello" }""",
      None
    )

    val coverageFile = serialize.Serializer.coverageFile(basePath)
    val compiler = ScoverageCompiler(basePath = basePath)

    compiler.compileSourceFiles(compilation1.file)
    compiler.assertNoErrors()

    val coverage = serialize.Serializer.deserialize(coverageFile, basePath)

    locally {
      val classNames = coverage.statements.map(_.location.className).toSet
      assertEquals(
        classNames,
        Set("Second"),
        s"First class should not be in coverage, but found: ${classNames.mkString(", ")}"
      )
    }
  }
}
