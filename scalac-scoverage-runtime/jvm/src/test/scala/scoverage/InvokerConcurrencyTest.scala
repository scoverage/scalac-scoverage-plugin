package scoverage

import java.io.File
import java.util.concurrent.Executors

import org.scalatest.{BeforeAndAfter, FunSuite}

import scala.concurrent._
import scala.concurrent.duration._

/**
 * Verify that [[Invoker.invoked()]] is thread-safe
 */
class InvokerConcurrencyTest extends FunSuite with BeforeAndAfter {

  implicit val executor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(8))

  val measurementDir = new File("target/invoker-test.measurement")

  before {
    deleteMeasurementFiles()
    measurementDir.mkdirs()
  }

  test("calling Invoker.invoked on multiple threads does not corrupt the measurement file") {

    val testIds: Set[Int] = (1 to 1000).toSet

    // Create 1k "invoked" calls on the common thread pool, to stress test
    // the method
    val futures: List[Future[Unit]] = testIds.map { i: Int =>
      Future {
        Invoker.invoked(i, measurementDir.toString)
      }
    }.toList

    futures.foreach(Await.result(_, 1.second))

    // Now verify that the measurement file is not corrupted by loading it
    val measurementFiles = Invoker.findMeasurementFiles(measurementDir)
    val idsFromFile = Invoker.invoked(measurementFiles).toSet

    idsFromFile === testIds
  }

  after {
    deleteMeasurementFiles()
    measurementDir.delete()
  }

  private def deleteMeasurementFiles(): Unit = {
    if (measurementDir.isDirectory)
      measurementDir.listFiles().foreach(_.delete())
  }
}
