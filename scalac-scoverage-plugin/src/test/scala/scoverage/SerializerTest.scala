package scoverage

import java.io.StringWriter

import org.scalatest.OneInstancePerTest
import org.scalatest.funsuite.AnyFunSuite

class SerializerTest extends AnyFunSuite with OneInstancePerTest {

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
          "mypath"
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
    val expected = s"""# Coverage data, format version: 2.0
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
                      |""".stripMargin.replaceAll("(\r\n)|\n|\r", "\n")
    val writer = new StringWriter() // TODO-use UTF-8
    val actual = Serializer.serialize(coverage, writer)
    assert(expected === writer.toString)
  }

  test("coverage should be deserializable from plain text") {
    val input = s"""# Coverage data, format version: 2.0
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
                   |""".stripMargin.split("(\r\n)|\n|\r").iterator
    val statements = List(
      Statement(
        Location(
          "org.scoverage",
          "test",
          "org.scoverage.test",
          ClassType.Trait,
          "mymethod",
          "mypath"
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
    val coverage = Serializer.deserialize(input)
    assert(statements === coverage.statements.toList)
  }
}
