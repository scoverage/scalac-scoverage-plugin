package scoverage

import java.io.{File, FileWriter}
import java.util.UUID

import org.scalatest.OneInstancePerTest
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

/** @author Stephen Samuel */
class IOUtilsTest extends AnyFreeSpec with OneInstancePerTest with Matchers {

  "io utils" - {
    "should parse measurement files" in {
      val file = File.createTempFile("scoveragemeasurementtest", "txt")
      val writer = new FileWriter(file)
      writer.write("1\n5\n9\n\n10\n")
      writer.close()
      val invoked = IOUtils.invoked(Seq(file))
      assert(invoked === Set(1, 5, 9, 10))

      file.delete()
    }
    "should parse multiple measurement files" in {
      // clean up any existing measurement files
      for ( file <- IOUtils.findMeasurementFiles(IOUtils.getTempDirectory) )
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
      assert(invoked === Set(1, 2, 5, 7, 9, 10, 14))

      file1.delete()
      file2.delete()
    }
    "should deep search for scoverage-data directories" in {
      // create new folder to hold all our data
      val base = new File(IOUtils.getTempDirectory, UUID.randomUUID.toString)

      val dataDir1 = new File(base, Constants.DataDir)
      dataDir1.mkdirs() shouldBe true

      val subDir = new File(base, UUID.randomUUID.toString)
      val dataDir2 = new File(subDir, Constants.DataDir)
      dataDir2.mkdirs() shouldBe true

      val subSubDir = new File(subDir, UUID.randomUUID.toString)
      val dataDir3 = new File(subSubDir, Constants.DataDir)
      dataDir3.mkdirs() shouldBe true

      val dataDirs = IOUtils.scoverageDataDirsSearch(base)
      dataDirs should contain only (dataDir1, dataDir2, dataDir3)
    }
  }
}
