package scoverage.reporter

import java.io.File
import java.io.FileWriter
import java.util.UUID

import munit.FunSuite
import scoverage.domain.Constants

/** @author Stephen Samuel */
class IOUtilsTest extends FunSuite {

  test("should parse measurement files") {
    val file = File.createTempFile("scoveragemeasurementtest", "txt")
    val writer = new FileWriter(file)
    writer.write("1\n5\n9\n\n10\n")
    writer.close()
    val invoked = IOUtils.invoked(Seq(file))
    assertEquals(invoked, Set((1, ""), (5, ""), (9, ""), (10, "")))

    file.delete()
  }

  test("should parse multiple measurement files") {
    // clean up any existing measurement files
    for (file <- IOUtils.findMeasurementFiles(IOUtils.getTempDirectory))
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
    val invoked = IOUtils.invoked(files.toIndexedSeq)
    assertEquals(
      invoked,
      Set(
        (1, ""),
        (2, ""),
        (5, ""),
        (7, ""),
        (9, ""),
        (10, ""),
        (14, "")
      )
    )

    file1.delete()
    file2.delete()
  }
  test("should deep search for scoverage-data directories") {
    // create new folder to hold all our data
    val base = new File(IOUtils.getTempDirectory, UUID.randomUUID.toString)

    val dataDir1 = new File(base, Constants.DataDir)
    assertEquals(dataDir1.mkdirs(), true)

    val subDir = new File(base, UUID.randomUUID.toString)
    val dataDir2 = new File(subDir, Constants.DataDir)
    assertEquals(dataDir2.mkdirs(), true)

    val subSubDir = new File(subDir, UUID.randomUUID.toString)
    val dataDir3 = new File(subSubDir, Constants.DataDir)
    assertEquals(dataDir3.mkdirs(), true)

    val dataDirs = IOUtils.scoverageDataDirsSearch(base)
    assert(dataDirs.contains(dataDir1))
    assert(dataDirs.contains(dataDir2))
    assert(dataDirs.contains(dataDir3))
    assertEquals(dataDirs.size, 3)
  }
}
