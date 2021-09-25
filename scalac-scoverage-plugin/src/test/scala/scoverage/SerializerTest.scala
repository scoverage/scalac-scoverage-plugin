package scoverage

import java.io.File
import java.io.StringWriter

import org.scalatest.OneInstancePerTest
import org.scalatest.funsuite.AnyFunSuite

class SerializerTest extends AnyFunSuite with OneInstancePerTest {
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
      s"""# Coverage data, format version: ${Serializer.coverageDataFormatVersion}
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
    val writer = new StringWriter() //TODO-use UTF-8
    val actual = Serializer.serialize(coverage, writer, sourceRoot)
    assert(expected === writer.toString)
  }

  test("coverage should be deserializable from plain text") {
    val input =
      s"""# Coverage data, format version: ${Serializer.coverageDataFormatVersion}
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
    val coverage = Serializer.deserialize(input, sourceRoot)
    assert(statements === coverage.statements.toList)
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
      s"""# Coverage data, format version: ${Serializer.coverageDataFormatVersion}
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
    val writer = new StringWriter() //TODO-use UTF-8
    val actual = Serializer.serialize(coverage, writer, sourceRoot)
    assert(expected === writer.toString)
  }

  test("coverage should deserialize sourcePath by prefixing cwd") {
    val input =
      s"""# Coverage data, format version: ${Serializer.coverageDataFormatVersion}
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
    val coverage = Serializer.deserialize(input, sourceRoot)
    assert(statements === coverage.statements.toList)
  }
}
