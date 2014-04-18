package scoverage

import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSuite, OneInstancePerTest}
import java.io.{FileWriter, File}

/** @author Stephen Samuel */
class IOUtilsTest extends FunSuite with MockitoSugar with OneInstancePerTest {

  test("io utils should parse measurement file") {
    val file = File.createTempFile("scoveragemeasurementtest", "txt")
    val writer = new FileWriter(file)
    writer.write("1;5;9;;10;")
    writer.close()
    val invoked = IOUtils.invoked(Seq(file))
    assert(invoked.toSet === Set(1, 5, 9, 10))

    file.delete()
  }

  test("io utils should parse multiple measurement files") {
    val file1 = File.createTempFile("scoverage.measurements.1", "txt")
    val writer1 = new FileWriter(file1)
    writer1.write("1;5;9;;10;")
    writer1.close()

    val file2 = File.createTempFile("scoverage.measurements.2", "txt")
    val writer2 = new FileWriter(file2)
    writer2.write("1;7;14;;2;")
    writer2.close()

    val files = IOUtils.findMeasurementFiles(file1.getParent)
    val invoked = IOUtils.invoked(files)
    assert(invoked.toSet === Set(1, 2, 5, 7, 9, 10, 14))

    file1.delete()
    file2.delete()
  }
}
