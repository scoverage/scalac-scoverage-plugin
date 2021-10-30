package scoverage.reporter

import java.io.File
import java.io.StringWriter

import munit.FunSuite
import scoverage.domain.ClassType
import scoverage.domain.Coverage
import scoverage.domain.Location
import scoverage.domain.Statement

class SerializerTest extends FunSuite {
  private val sourceRoot = new File(".").getCanonicalFile()

  test("coverage should be serializable into plain text") {
    val coverage = Coverage()
    coverage.add(
      Statement(
        Location(
          "org.scoverage",
          "test",
          "org.scoverage.test",
          ClassType.Trait,
          "mymethod",
          new File(sourceRoot, "mypath").getAbsolutePath()
        ),
        14,
        100,
        200,
        4,
        "def test : String",
        "test",
        "DefDef",
        true,
        1
      )
    )
    val expected =
      s"""# Coverage data, format version: ${Deserializer.coverageDataFormatVersion}
         |# Statement data:
         |# - id
         |# - source path
         |# - package name
         |# - class name
         |# - class type (Class, Object or Trait)
         |# - full class name
         |# - method name
         |# - start offset
         |# - end offset
         |# - line number
         |# - symbol name
         |# - tree name
         |# - is branch
         |# - invocations count
         |# - is ignored
         |# - description (can be multi-line)
         |# '\f' sign
         |# ------------------------------------------
         |14
         |mypath
         |org.scoverage
         |test
         |Trait
         |org.scoverage.test
         |mymethod
         |100
         |200
         |4
         |test
         |DefDef
         |true
         |1
         |false
         |def test : String
         |\f
         |""".stripMargin
    val writer = new StringWriter() // TODO-use UTF-8
    val actual = Deserializer.serialize(coverage, writer, sourceRoot)
    assertEquals(expected, writer.toString)
  }

  test("coverage should be deserializable from plain text") {
    val input =
      s"""# Coverage data, format version: ${Deserializer.coverageDataFormatVersion}
         |# Statement data:
         |# - id
         |# - source path
         |# - package name
         |# - class name
         |# - class type (Class, Object or Trait)
         |# - full class name
         |# - method name
         |# - start offset
         |# - end offset
         |# - line number
         |# - symbol name
         |# - tree name
         |# - is branch
         |# - invocations count
         |# - is ignored
         |# - description (can be multi-line)
         |# '\f' sign
         |# ------------------------------------------
         |14
         |mypath
         |org.scoverage
         |test
         |Trait
         |org.scoverage.test
         |mymethod
         |100
         |200
         |4
         |test
         |DefDef
         |true
         |1
         |false
         |def test : String
         |\f
         |""".stripMargin
        .split(System.lineSeparator())
        .iterator
    val statements = List(
      Statement(
        Location(
          "org.scoverage",
          "test",
          "org.scoverage.test",
          ClassType.Trait,
          "mymethod",
          new File(sourceRoot, "mypath").getAbsolutePath()
        ),
        14,
        100,
        200,
        4,
        "def test : String",
        "test",
        "DefDef",
        true,
        1
      )
    )
    val coverage = Deserializer.deserialize(input, sourceRoot)
    assertEquals(statements, coverage.statements.toList)
  }
  test("coverage should serialize sourcePath relatively") {
    val coverage = Coverage()
    coverage.add(
      Statement(
        Location(
          "org.scoverage",
          "test",
          "org.scoverage.test",
          ClassType.Trait,
          "mymethod",
          new File(sourceRoot, "mypath").getAbsolutePath()
        ),
        14,
        100,
        200,
        4,
        "def test : String",
        "test",
        "DefDef",
        true,
        1
      )
    )
    val expected =
      s"""# Coverage data, format version: ${Deserializer.coverageDataFormatVersion}
         |# Statement data:
         |# - id
         |# - source path
         |# - package name
         |# - class name
         |# - class type (Class, Object or Trait)
         |# - full class name
         |# - method name
         |# - start offset
         |# - end offset
         |# - line number
         |# - symbol name
         |# - tree name
         |# - is branch
         |# - invocations count
         |# - is ignored
         |# - description (can be multi-line)
         |# '\f' sign
         |# ------------------------------------------
         |14
         |mypath
         |org.scoverage
         |test
         |Trait
         |org.scoverage.test
         |mymethod
         |100
         |200
         |4
         |test
         |DefDef
         |true
         |1
         |false
         |def test : String
         |\f
         |""".stripMargin
    val writer = new StringWriter() // TODO-use UTF-8
    val actual = Deserializer.serialize(coverage, writer, sourceRoot)
    assertEquals(expected, writer.toString)
  }

  test("coverage should deserialize sourcePath by prefixing cwd") {
    val input =
      s"""# Coverage data, format version: ${Deserializer.coverageDataFormatVersion}
         |# Statement data:
         |# - id
         |# - source path
         |# - package name
         |# - class name
         |# - class type (Class, Object or Trait)
         |# - full class name
         |# - method name
         |# - start offset
         |# - end offset
         |# - line number
         |# - symbol name
         |# - tree name
         |# - is branch
         |# - invocations count
         |# - is ignored
         |# - description (can be multi-line)
         |# '\f' sign
         |# ------------------------------------------
         |14
         |mypath
         |org.scoverage
         |test
         |Trait
         |org.scoverage.test
         |mymethod
         |100
         |200
         |4
         |test
         |DefDef
         |true
         |1
         |false
         |def test : String
         |\f
         |""".stripMargin.split(System.lineSeparator()).iterator
    val statements = List(
      Statement(
        Location(
          "org.scoverage",
          "test",
          "org.scoverage.test",
          ClassType.Trait,
          "mymethod",
          new File(sourceRoot, "mypath").getCanonicalPath().toString()
        ),
        14,
        100,
        200,
        4,
        "def test : String",
        "test",
        "DefDef",
        true,
        1
      )
    )
    val coverage = Deserializer.deserialize(input, sourceRoot)
    assertEquals(statements, coverage.statements.toList)
  }
}
