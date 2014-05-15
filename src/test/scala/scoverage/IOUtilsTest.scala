package scoverage

import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSuite, OneInstancePerTest}
import java.io.{FileWriter, File}
import scala.xml.Utility
import org.apache.commons.io.FileUtils

/** @author Stephen Samuel */
class IOUtilsTest extends FunSuite with MockitoSugar with OneInstancePerTest {

  test("coverage should be serializable into xml") {
    val coverage = Coverage()
    coverage.add(
      MeasuredStatement(
        "mysource",
        Location("org.scoverage", "test", ClassType.Trait, "mymethod"),
        14, 100, 200, 4, "def test : String", "test", "DefDef", true, 32
      )
    )
    val expected = <statements>
      <statement>
        <source>mysource</source> <package>org.scoverage</package> <class>test</class> <classType>Trait</classType> <method>mymethod</method> <id>14</id> <start>100</start> <end>200</end> <line>4</line> <description>def test : String</description> <symbolName>test</symbolName> <treeName>DefDef</treeName> <branch>true</branch> <count>32</count>
      </statement>
    </statements>
    val actual = IOUtils.serialize(coverage)
    assert(Utility.trim(expected) === Utility.trim(actual))
  }

  test("coverage should be deserializable from xml") {
    val input = <statements>
      <statement>
        <source>mysource</source> <package>org.scoverage</package> <class>test</class> <classType>Trait</classType> <method>mymethod</method> <id>14</id> <start>100</start> <end>200</end> <line>4</line> <description>def test : String</description> <symbolName>test</symbolName> <treeName>DefDef</treeName> <branch>true</branch> <count>32</count>
      </statement>
    </statements>
    val statements = List(MeasuredStatement(
      "mysource",
      Location("org.scoverage", "test", ClassType.Trait, "mymethod"),
      14, 100, 200, 4, "def test : String", "test", "DefDef", true, 32
    ))
    val coverage = IOUtils.deserialize(input.toString())
    assert(statements === coverage.statements.toList)
  }


  test("io utils should parse measurement file") {
    val file = File.createTempFile("scoveragemeasurementtest", "txt")
    val writer = new FileWriter(file)
    writer.write("1\n5\n9\n\n10\n")
    writer.close()
    val invoked = IOUtils.invoked(Seq(file))
    assert(invoked.toSet === Set(1, 5, 9, 10))

    file.delete()
  }

  test("io utils should parse multiple measurement files") {

    // clean up any existing measurement files
    for ( file <- IOUtils.findMeasurementFiles(FileUtils.getTempDirectoryPath) )
      file.delete()

    val file1 = File.createTempFile("scoverage.measurements.1", "txt")
    val writer1 = new FileWriter(file1)
    writer1.write("1\n5\n9\n\n10\n")
    writer1.close()

    val file2 = File.createTempFile("scoverage.measurements.2", "txt")
    val writer2 = new FileWriter(file2)
    writer2.write("1\n7\n14\n\n2\n")
    writer2.close()

    val files = IOUtils.findMeasurementFiles(file1.getParent)
    val invoked = IOUtils.invoked(files)
    assert(invoked.toSet === Set(1, 2, 5, 7, 9, 10, 14))

    file1.delete()
    file2.delete()
  }
}
