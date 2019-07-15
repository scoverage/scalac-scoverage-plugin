package scoverage

import org.scalatest.{BeforeAndAfter, FunSuite}
import scoverage.Platform.File

/**
 * Verify that [[Invoker.invokedWriteToClasspath()]] works as expected.
 */
class InvokerUseEnvironmentTest extends FunSuite with BeforeAndAfter {


  val targetIds = Array(
    new File("target_id_0"),
    new File("target_id_1")
  )

  before {
    System.setProperty("scoverage_measurement_path","scoverage_java_prop")
  }

  test("calling Invoker.invokedUseEnvironments puts the data on sys property scoverage_measurement_path") {

    val testIds: Set[Int] = (1 to 10).toSet

    testIds.map { i: Int => Invoker.invokedWriteToClasspath(i, targetIds(i % 2).toString)}

    // Verify that [Invoker.dataDir] has been set correctly.
    assert(Invoker.dataDir == System.getProperty("scoverage_measurement_path"))

    // Verify measurements went to correct directory under the system property.
    val dir0 = s"${Invoker.dataDir}/${targetIds(0).toString}"
    val measurementFiles3 = Invoker.findMeasurementFiles(dir0)
    val idsFromFile3 = Invoker.invoked(measurementFiles3.toIndexedSeq)
    assert (idsFromFile3 == testIds.filter { i: Int => i % 2 == 0})


    val dir1 = s"${Invoker.dataDir}/${targetIds(1).toString}"
    val measurementFiles4 = Invoker.findMeasurementFiles(dir1)
    val idsFromFile4 = Invoker.invoked(measurementFiles4.toIndexedSeq)
    assert (idsFromFile4 == testIds.filter { i: Int => i % 2 == 1})

  }

  after {
    deleteMeasurementFolders()
  }


  private def deleteMeasurementFolders(): Unit = {
    val d = s"${Invoker.dataDir}"
    targetIds.foreach (i => {
      val f = new File(s"$d/${i.toString}")
      if (f.isDirectory)
        f.listFiles().foreach(_.delete())
    })

    val f2 = new File(d)
    if(f2.isDirectory)
      f2.listFiles().foreach(_.delete())
    f2.delete()
  }


}
