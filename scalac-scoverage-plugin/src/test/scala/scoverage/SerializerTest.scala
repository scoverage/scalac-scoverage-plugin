package scoverage

import java.io.StringWriter

import org.scalatest.{OneInstancePerTest, FunSuite}
import org.scalatest.mock.MockitoSugar

import scala.xml.Utility

class SerializerTest extends FunSuite with MockitoSugar with OneInstancePerTest {

  test("coverage should be serializable into xml") {
    val coverage = Coverage()
    coverage.add(
      Statement(
        "mysource",
        Location("org.scoverage", "test", "test", ClassType.Trait, "mymethod", "mypath"),
        14, 100, 200, 4, "def test : String", "test", "DefDef", true, 32
      )
    )
    val expected = <statements>
      <statement>
        <source>mysource</source> <package>org.scoverage</package> <class>test</class> <classType>Trait</classType> <topLevelClass>test</topLevelClass> <method>mymethod</method> <path>mypath</path> <id>14</id> <start>100</start> <end>200</end> <line>4</line> <description>def test : String</description> <symbolName>test</symbolName> <treeName>DefDef</treeName> <branch>true</branch> <count>32</count> <ignored>false</ignored>
      </statement>
    </statements>
    val writer = new StringWriter()
    val actual = Serializer.serialize(coverage, writer)
    assert(Utility.trim(expected) === Utility.trim(xml.XML.loadString(writer.toString)))
  }

  test("coverage should be deserializable from xml") {
    val input = <statements>
      <statement>
        <source>mysource</source> <package>org.scoverage</package> <class>test</class> <classType>Trait</classType> <topLevelClass>test</topLevelClass> <method>mymethod</method> <path>mypath</path> <id>14</id> <start>100</start> <end>200</end> <line>4</line> <description>def test : String</description> <symbolName>test</symbolName> <treeName>DefDef</treeName> <branch>true</branch> <count>32</count> <ignored>false</ignored>
      </statement>
    </statements>
    val statements = List(Statement(
      "mysource",
      Location("org.scoverage", "test", "test", ClassType.Trait, "mymethod", "mypath"),
      14, 100, 200, 4, "def test : String", "test", "DefDef", true, 32
    ))
    val coverage = Serializer.deserialize(input.toString())
    assert(statements === coverage.statements.toList)
  }
}
