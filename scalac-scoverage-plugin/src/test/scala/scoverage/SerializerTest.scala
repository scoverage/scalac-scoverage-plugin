package scoverage

import java.io.StringWriter
import java.io.File

import org.scalatest.{OneInstancePerTest, FunSuite}

class SerializerTest extends FunSuite with OneInstancePerTest {

  test("coverage should be serializable into plain text") {
    val coverage = Coverage()
    coverage.add(
      Statement(
        Location("org.scoverage", "test", "org.scoverage.test", ClassType.Trait, "mymethod", "mypath"),
        14, 100, 200, 4, "def test : String", "test", "DefDef", true, 1
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
        |""".stripMargin
    val writer = new StringWriter()//TODO-use UTF-8
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
        |""".stripMargin.split("\n").iterator
    val statements = List(Statement(
      Location("org.scoverage", "test", "org.scoverage.test", ClassType.Trait, "mymethod", new File("mypath").getCanonicalPath),
      14, 100, 200, 4, "def test : String", "test", "DefDef", true, 1
    ))
    val coverage = Serializer.deserialize(input)
    assert(statements === coverage.statements.toList)
  }

  test("coverage should serialize sourcePath relatively") {
    val coverage = Coverage()
    coverage.add(
      Statement(
        Location("org.scoverage", "test", "org.scoverage.test", ClassType.Trait, "mymethod", new File(".").getCanonicalPath + File.separator + "mypath"),
        14, 100, 200, 4, "def test : String", "test", "DefDef", true, 1
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
                      |""".stripMargin
    val writer = new StringWriter()//TODO-use UTF-8
    val actual = Serializer.serialize(coverage, writer)
    assert(expected === writer.toString)
  }

  test("coverage should deserialize sourcePath by prefixing cwd") {
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
                   |""".stripMargin.split("\n").iterator
    val statements = List(Statement(
      Location("org.scoverage", "test", "org.scoverage.test", ClassType.Trait, "mymethod", new File(".").getCanonicalPath + File.separator + "mypath"),
      14, 100, 200, 4, "def test : String", "test", "DefDef", true, 1
    ))
    val coverage = Serializer.deserialize(input)
    assert(statements === coverage.statements.toList)
  }
}
