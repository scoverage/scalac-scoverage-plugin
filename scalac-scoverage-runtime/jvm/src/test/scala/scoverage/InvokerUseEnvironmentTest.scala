package scoverage

import java.nio.file.{Files, Paths}
import org.scalatest.{BeforeAndAfter, FunSuite}
import scoverage.Platform.File

/**
 * Verify that [[Invoker.invokedWriteToClasspath()]] works as expected.
 */
class InvokerUseEnvironmentTest extends FunSuite with BeforeAndAfter {


  val instrumentsDir = Array(
    new File("target/invoker-test.instrument0"),
    new File("target/invoker-test.instrument1")
  )

  before {
    deleteInstrumentFiles()
    instrumentsDir.foreach(_.mkdirs())
    createNewInstruments()
  }

  test("calling Invoker.invokedUseEnvironments puts the data on env var SCOVERAGE_MEASUREMENT_PATH" +
    " and copies coverage files from instruments directory.") {

    val testIds: Set[Int] = (1 to 10).toSet

    testIds.map { i: Int => Invoker.invokedWriteToClasspath(i, instrumentsDir(i % 2).toString)}

    // Verify measurements went to correct directory under the environment variable.
    val dir0 = s"${System.getenv("SCOVERAGE_MEASUREMENT_PATH")}/${Invoker.md5HashString(instrumentsDir(0).toString)}"
    val measurementFiles3 = Invoker.findMeasurementFiles(dir0)
    val idsFromFile3 = Invoker.invoked(measurementFiles3.toIndexedSeq)
    idsFromFile3 === testIds.filter { i: Int => i % 2 == 0}


    val dir1 = s"${System.getenv("SCOVERAGE_MEASUREMENT_PATH")}/${Invoker.md5HashString(instrumentsDir(1).toString)}"
    val measurementFiles4 = Invoker.findMeasurementFiles(dir1)
    val idsFromFile4 = Invoker.invoked(measurementFiles4.toIndexedSeq)
    idsFromFile4 === testIds.filter { i: Int => i % 2 == 1}

    // Verify that coverage files have been copied correctly.
    assert(Files.exists(Paths.get(s"$dir0/scoverage.coverage")))
    assert(Files.exists(Paths.get(s"$dir1/scoverage.coverage")))
  }

  after {
    deleteInstrumentFiles()
    instrumentsDir.foreach(_.delete())
    deleteMeasurementFolders()
  }

  private def deleteInstrumentFiles(): Unit = {
    instrumentsDir.foreach((md) => {
      if (md.isDirectory)
        md.listFiles().foreach(_.delete())
    })
  }

  private def deleteMeasurementFolders(): Unit = {
    val d = s"${System.getenv("SCOVERAGE_MEASUREMENT_PATH")}"
    instrumentsDir.foreach (i => {
      val f = new File(s"$d/${Invoker.md5HashString(i.toString)}")
      if (f.isDirectory)
        f.listFiles().foreach(_.delete())
    })

    val f2 = new File(d)
    if(f2.isDirectory)
      f2.listFiles().foreach(_.delete())
    f2.delete()
  }

  private def createNewInstruments(): Unit = {
    new File("target/invoker-test.instrument0/scoverage.coverage").createNewFile()
    new File("target/invoker-test.instrument1/scoverage.coverage").createNewFile()
  }

}
